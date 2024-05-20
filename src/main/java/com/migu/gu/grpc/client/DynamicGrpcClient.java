package com.migu.gu.grpc.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.*;
import com.migu.gu.biz.dto.GrpcField;
import com.migu.gu.biz.req.GrpcReq;
import com.migu.gu.biz.req.ProxyReq;
import com.migu.gu.biz.res.DescriptorRes;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import io.grpc.reflection.v1alpha.FileDescriptorResponse;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE;
import static io.grpc.reflection.v1alpha.ServerReflectionResponse.MessageResponseCase.FILE_DESCRIPTOR_RESPONSE;

/**
 * A program that generates dynamic data based on the Descriptors descriptor to generate a dynamic rpc invocation
 *
 * @Author kkmigu
 */
@Component
@Slf4j
public class DynamicGrpcClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Utilize the Server Reflection feature of gRPC for dynamic invocation,
     * which allows sending requests to the gRPC server based on the given service methods and message types, and handling the server's responses and error scenarios.
     *
     * @param grpcReq
     * @return
     */
    public DeferredResult<ResponseEntity<Object>> reflectGrpcCall(GrpcReq grpcReq) {
        if (grpcReq.getMessage() == null) {
            grpcReq.setMessage("Empty");
        }
        if (grpcReq.getFieldList() == null) {
            grpcReq.setFieldList(new ArrayList<>());
        }
        final String fullServiceMethod = grpcReq.getFullService() + "." + grpcReq.getMethod();
        final DeferredResult<ResponseEntity<Object>> reflectGrpcCallResponse = new DeferredResult<>();
        ManagedChannel channel = null;
        try {
            // create a latch to wait for the server's response
            CountDownLatch latch = new CountDownLatch(1);

            // create a channel to the grpc server
            channel = processManagedChannel(grpcReq).build();
            ServerReflectionGrpc.ServerReflectionStub reflectionStub = ServerReflectionGrpc.newStub(channel);
            // create a StreamObserver to receive the server's response
            StreamObserver<ServerReflectionResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(ServerReflectionResponse response) {
                    try {
                        Descriptors.FileDescriptor fileDescriptor = getFileDescriptor(response, reflectGrpcCallResponse);
                        if (fileDescriptor == null) return;
                        // filter service method
                        Descriptors.MethodDescriptor originMethodDescriptor = null;
                        originMethodDescriptor = fileDescriptor.getServices().stream()
                                .filter(serviceDescriptor -> serviceDescriptor.getFullName().equals(grpcReq.getFullService()))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("["+grpcReq.getFullService() + "] service not found"))
                                .getMethods().stream()
                                .filter(methodDescriptor -> methodDescriptor.getFullName().equals(fullServiceMethod))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("method not found"));
                        // construct DynamicMessage
                        Descriptors.Descriptor reqMessageDescriptor = fileDescriptor.getMessageTypes().stream().filter(e -> e.getName().equals(grpcReq.getMessage())).findFirst().orElseThrow(() -> new RuntimeException("message not found"));
                        DynamicMessage reqMessage = constructFields(reqMessageDescriptor
                                , originMethodDescriptor, grpcReq.getFieldList());
                        UnknownFieldSet unknownFieldSet = (UnknownFieldSet) ClientCalls.blockingUnaryCall(reflectionStub.getChannel(), getMethodDescriptor(originMethodDescriptor), reflectionStub.getCallOptions(), reqMessage);
                        DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(originMethodDescriptor.getOutputType());
                        DynamicMessage resultMessage = messageBuilder.buildPartial().getParserForType().parseFrom(unknownFieldSet.toByteArray());
                        String result = new String(TextFormat.unescapeBytes(resultMessage.toString()).toByteArray());
                        reflectGrpcCallResponse.setResult(ResponseEntity.ok(result));
                    } catch (RuntimeException | Descriptors.DescriptorValidationException | IOException e) {
                        log.error("grpc call error: {}", e.getMessage());
                        reflectGrpcCallResponse.setErrorResult(e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    reflectGrpcCallResponse.setErrorResult(ResponseEntity.status(HttpStatusCode.valueOf(HttpServletResponse.SC_BAD_REQUEST)).body(throwable.getMessage()));
                    log.error("grpc call error: {}", throwable.getMessage());
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    latch.countDown();
                }
            };
            send(fullServiceMethod, reflectionStub, responseObserver);
            // wait for the server's response
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception exception) {
            log.error("grpc call error: {}", exception.getMessage());
            reflectGrpcCallResponse.setErrorResult(ResponseEntity.status(HttpStatusCode.valueOf(HttpServletResponse.SC_BAD_REQUEST)).body(exception.getMessage()));
        } finally {
            if (channel != null) channel.shutdownNow();
        }
        return reflectGrpcCallResponse;
    }

    /**
     * Query the descriptor of the gRPC server based on the given service method and message type,
     *
     * @param grpcReq
     * @return
     */
    public DeferredResult<ResponseEntity<List<DescriptorRes>>> queryDescriptor(GrpcReq grpcReq) {
        final DeferredResult<ResponseEntity<List<DescriptorRes>>> reflectGrpcCallResponse = new DeferredResult<>();
        ManagedChannel channel = null;
        try {
            // create a latch to wait for the server's response
            CountDownLatch latch = new CountDownLatch(1);

            // create a channel to the grpc server
            channel = processManagedChannel(grpcReq).build();
            ServerReflectionGrpc.ServerReflectionStub reflectionStub = ServerReflectionGrpc.newStub(channel);
            // create a StreamObserver to receive the server's response
            StreamObserver<ServerReflectionResponse> responseObserver = new StreamObserver<>() {
                @SneakyThrows
                @Override
                public void onNext(ServerReflectionResponse response) {
                    // support
                    Descriptors.FileDescriptor fileDescriptor = getFileDescriptor(response, reflectGrpcCallResponse);
                    if (fileDescriptor == null) return;

                    List<DescriptorRes> serviceList = fileDescriptor.getServices().stream()
                            .filter(serviceDescriptor -> serviceDescriptor.getFullName().equals(grpcReq.getFullService()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("["+grpcReq.getFullService() + "] service not found"))
                            .getMethods().stream().map(e -> {
                                DescriptorRes descriptorRes = new DescriptorRes();
                                descriptorRes.setMethod(e.getName());
                                descriptorRes.setMessage(e.getInputType().getName());
                                descriptorRes.setFieldList(e.getInputType().getFields()
                                        .stream()
                                        .map(DynamicGrpcClient::processField)
                                        .collect(Collectors.toList()));
                                return descriptorRes;
                            }).collect(Collectors.toList());
                    reflectGrpcCallResponse.setResult(ResponseEntity.ok(serviceList));
                }

                @Override
                public void onError(Throwable throwable) {
                    reflectGrpcCallResponse.setErrorResult(ResponseEntity.status(HttpStatusCode.valueOf(HttpServletResponse.SC_BAD_REQUEST)).body(throwable.getMessage()));
                    log.error("grpc call error: {}", throwable.getMessage());
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    latch.countDown();
                }
            };
            send(grpcReq.getFullService(), reflectionStub, responseObserver);
            // wait for the server's response
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception exception) {
            log.error("grpc call error: {}", exception.getMessage());
            reflectGrpcCallResponse.setErrorResult(ResponseEntity.status(HttpStatusCode.valueOf(HttpServletResponse.SC_BAD_REQUEST)).body(exception.getMessage()));
        } finally {
            if (channel != null) channel.shutdownNow();
        }
        return reflectGrpcCallResponse;
    }

    private static GrpcField processField(Descriptors.FieldDescriptor item) {
        GrpcField field = new GrpcField();
        field.setName(item.getName());
        field.setType(item.getType().name());
        if (item.getJavaType() == MESSAGE && !item.getMessageType().getFields().isEmpty()) {
            List<GrpcField> childFields = new ArrayList<>();
            item.getMessageType().getFields().forEach(e -> {
                childFields.add(processField(e));
            });
            field.setChildFields(childFields);
        }
        return field;
    }


    /**
     * Construct a gRPC ManagedChannelBuilder object with the given parameters to create a connection to the gRPC server.
     * As needed, it can be configured to use either plaintext or encrypted communication, as well as to connect through a proxy server.
     */
    private static ManagedChannelBuilder<?> processManagedChannel(GrpcReq grpcReq) throws Exception {
        ManagedChannelBuilder<?> managedChannelBuilder = ManagedChannelBuilder.forAddress(grpcReq.getAddress(), grpcReq.getPort());
        switch (grpcReq.getTransportType()) {
            case PLAINTEXT:
                managedChannelBuilder = managedChannelBuilder.usePlaintext();
                break;
            case TLS:
                SslContext sslContext = buildSslContext(grpcReq.getTrustCertCollectionFile(), grpcReq.getClientCertChainFile(), grpcReq.getClientPrivateKeyFile());
                managedChannelBuilder = NettyChannelBuilder.forAddress(grpcReq.getAddress(), grpcReq.getPort())
                        .negotiationType(NegotiationType.TLS)
                        .sslContext(sslContext);
                break;
            default:
                throw new IllegalArgumentException("unsupported transport type");
        }

        ProxyReq proxyReq = grpcReq.getProxyReq();
        if (proxyReq != null && proxyReq.getAddress() != null && !proxyReq.getAddress().isEmpty() && proxyReq.getPort() != null) {
            InetSocketAddress proxyAddress = new InetSocketAddress(proxyReq.getAddress(), proxyReq.getPort());
            managedChannelBuilder = managedChannelBuilder.proxyDetector(new ProxyDetector() {
                @Nullable
                @Override
                public ProxiedSocketAddress proxyFor(SocketAddress targetServerAddress) throws IOException {
                    if (!(targetServerAddress instanceof InetSocketAddress)) {
                        return null;
                    }
                    return HttpConnectProxiedSocketAddress.newBuilder()
                            .setTargetAddress((InetSocketAddress) targetServerAddress)
                            .setProxyAddress(proxyAddress)
                            .build();
                }
            });
        }
        return managedChannelBuilder;
    }

    /**
     * send request, query descriptor
     *
     * @param fullServiceMethod full Service Method , e.g. <package>.<service>[.<method>]
     * @param reflectionStub    ServerReflectionStub
     * @param responseObserver  StreamObserver
     */
    private static void send(String fullServiceMethod, ServerReflectionGrpc.ServerReflectionStub reflectionStub, StreamObserver<ServerReflectionResponse> responseObserver) {
        StreamObserver<ServerReflectionRequest> requestStreamObserver = reflectionStub.serverReflectionInfo(responseObserver);
        ServerReflectionRequest serverReflectionRequest = ServerReflectionRequest.newBuilder()
                .setFileContainingSymbol(fullServiceMethod)
                .build();
        requestStreamObserver.onNext(serverReflectionRequest);
        requestStreamObserver.onCompleted();
    }

    /**
     * Transform the original method descriptors into method descriptors suitable for gRPC Java client code,
     * enabling the client to use these descriptors for gRPC method invocations.
     *
     * @param originMethodDescriptor originMethodDescriptor
     * @return
     */
    private static MethodDescriptor getMethodDescriptor(Descriptors.MethodDescriptor originMethodDescriptor) {
        MethodDescriptor.Marshaller requestMarshaller = ProtoLiteUtils.marshaller(originMethodDescriptor.getInputType().toProto().getUnknownFields());

        MethodDescriptor.Marshaller responseMarshaller = ProtoLiteUtils.marshaller(originMethodDescriptor.getOutputType().toProto().getUnknownFields());

        MethodDescriptor methodDescriptor = MethodDescriptor.newBuilder()
                .setFullMethodName(originMethodDescriptor.getFullName().replace(".", "/"))
                .setSampledToLocalTracing(true)
                .setType(MethodDescriptor.MethodType.UNARY)
                .setRequestMarshaller(requestMarshaller)
                .setResponseMarshaller(responseMarshaller)
                .build();
        return methodDescriptor;
    }

    /**
     * construct DynamicMessage
     *
     * @param messageDescriptor messageDescriptor
     * @param methodDescriptor  methodDescriptor
     * @return
     * @throws InvalidProtocolBufferException
     */
    private static DynamicMessage constructFields(Descriptors.Descriptor messageDescriptor, Descriptors.MethodDescriptor methodDescriptor, List<GrpcField> grpcFields) throws InvalidProtocolBufferException {
        DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(messageDescriptor);
        grpcFields.forEach(e -> {
            Descriptors.FieldDescriptor fieldDescriptor = methodDescriptor.getInputType().findFieldByName(e.getName());
            switch (fieldDescriptor.getJavaType()) {
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE:
                case BOOLEAN:
                    messageBuilder.setField(fieldDescriptor, getActualField(e.getValue().toString(),fieldDescriptor.getJavaType()));
                    break;
                case MESSAGE:
                    DynamicMessage.Builder builder = messageBuilder.newBuilderForField(fieldDescriptor);

                    JsonNode jsonNode = OBJECT_MAPPER.valueToTree(e.getValue());
                    Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> item = fields.next();
                        Descriptors.FieldDescriptor fieldDesc = fieldDescriptor.getMessageType().findFieldByName(item.getKey());
                        builder.setField(fieldDesc,getActualField(item.getValue().textValue(), fieldDesc.getJavaType()));
                    }
                    messageBuilder.setField(fieldDescriptor, builder.build());
                    break;
                default:
                    messageBuilder.setField(fieldDescriptor, e.getValue());
                    break;
            }
        });
        return messageBuilder.build();
    }

    private static Object getActualField(String field, Descriptors.FieldDescriptor.JavaType javaType){
        switch (javaType) {
            case INT:
                return Integer.valueOf(field);
            case LONG:
                return Long.valueOf(field);
            case FLOAT:
                return Float.valueOf(field);
            case DOUBLE:
                return Double.valueOf(field);
            case BOOLEAN:
                return Boolean.valueOf(field);
            default:
                return field;
        }
    }

    /**
     * Construct an SSL context for establishing a secure connection between the client and the gRPC server.
     *
     * @param trustCertCollectionFile The trust certificate collection file
     * @param clientCertChainFile     The client certificate chain file
     * @param clientPrivateKeyFile    The client private key file
     * @return
     * @throws Exception
     */
    private static SslContext buildSslContext(MultipartFile trustCertCollectionFile,
                                              MultipartFile clientCertChainFile,
                                              MultipartFile clientPrivateKeyFile) throws Exception {

        SslContextBuilder builder = GrpcSslContexts.forClient();
        if (trustCertCollectionFile != null) {
            builder.trustManager(trustCertCollectionFile.getInputStream());
        }
        if (clientCertChainFile != null && clientPrivateKeyFile != null) {
            builder.keyManager(clientCertChainFile.getInputStream(), clientPrivateKeyFile.getInputStream());
        }
        return builder.build();
    }


    /**
     * Extracting file descriptors from the reflection response of the gRPC server for subsequent gRPC calls on the client-side.
     *
     * @throws InvalidProtocolBufferException
     * @throws Descriptors.DescriptorValidationException
     */
    private static <T> Descriptors.FileDescriptor getFileDescriptor(ServerReflectionResponse response, DeferredResult<ResponseEntity<T>> reflectGrpcCallResponse) throws InvalidProtocolBufferException, Descriptors.DescriptorValidationException {
        if (response.getMessageResponseCase() != FILE_DESCRIPTOR_RESPONSE) {
            reflectGrpcCallResponse.setErrorResult(ResponseEntity.status(HttpStatusCode.valueOf(HttpServletResponse.SC_BAD_REQUEST)).body("Service can not be found"));
            return null;
        }
        FileDescriptorResponse fileDescriptorResponse = response.getFileDescriptorResponse();
        if (fileDescriptorResponse.getFileDescriptorProtoList().isEmpty()) {
            reflectGrpcCallResponse.setErrorResult(ResponseEntity.status(HttpStatusCode.valueOf(200)).body("Service can not be found"));
            return null;
        }
        // parse fileDescriptorProtoBytes
        ByteString fileDescriptorProtoBytes = fileDescriptorResponse.getFileDescriptorProto(0);
        DescriptorProtos.FileDescriptorProto fileDescriptorProto = DescriptorProtos.FileDescriptorProto.parseFrom(fileDescriptorProtoBytes);
        return Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0]);
    }
}
