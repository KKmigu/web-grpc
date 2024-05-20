# Web GRPC

## Introduction

This is a project that calls GRPC services from a web ui.

## Using

一、Project Clone to Local

二、Start the project and access local services

<img src="https://raw.githubusercontent.com/KKmigu/image/master/web_grpc/index_1.png">

Suppose we have a grpc service that has a proto file like this

```protobuf
syntax = "proto3";
option java_package = "com.migu.wg.grpc.gen";
option java_outer_classname = "GreeterGrpcProtos";
option java_generic_services = true;

service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}
message HelloRequest {
  string name = 1;
  bool isMan = 2;
  int32 age = 3;
  Address address = 4;
}
message Address {
  string street = 1;
  string city = 2;
}

message HelloReply {
  string name = 1;
  bool isMan = 2;
  int32 age = 3;
  Address address = 4;
}
```

At this point you can request the grpc service ip, port, full service name in the WebGRPC interface, sample request:

<img src="https://raw.githubusercontent.com/KKmigu/image/master/web_grpc/index_2.png">

Then you get the grpc service method and its request field, and finally you can make a request to the service

<img src="https://raw.githubusercontent.com/KKmigu/image/master/web_grpc/index_3.png">

## License

Web GRPC is Open Source software released under the Apache 2.0 license.