package com.migu.gu.grpc.service;

import com.migu.gu.biz.req.GrpcReq;
import com.migu.gu.biz.res.DescriptorRes;
import com.migu.gu.grpc.client.DynamicGrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author kkmigu
 *
 * @Description
 */
@Component
public class GrpcService {

    @Resource
    private DynamicGrpcClient dynamicGrpcClient;

    public DeferredResult<ResponseEntity<Object>> call(GrpcReq grpcReq) {
        return dynamicGrpcClient.reflectGrpcCall(grpcReq);
    }

    public DeferredResult<ResponseEntity<List<DescriptorRes>>> queryDescriptor(GrpcReq grpcReq) {
        return dynamicGrpcClient.queryDescriptor(grpcReq);
    }
}
