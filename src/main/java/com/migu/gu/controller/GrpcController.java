package com.migu.gu.controller;

import com.migu.gu.biz.req.GrpcReq;
import com.migu.gu.biz.res.DescriptorRes;
import com.migu.gu.grpc.service.GrpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author kkmigu
 * @Description
 */
@RestController
@RequestMapping("/api/grpc")
@Slf4j
@Validated
public class GrpcController {
    @Resource
    private GrpcService grpcService;


    @PostMapping("/call")
    public DeferredResult<ResponseEntity<Object>> call(@RequestPart("grpcReq") @Validated GrpcReq grpcReq
            , @RequestPart(name = "trustCertCollectionFile",required = false) MultipartFile trustCertCollectionFile
            , @RequestPart(name = "clientCertChainFile",required = false) MultipartFile clientCertChainFile
            , @RequestPart(name = "clientPrivateKeyFile",required = false) MultipartFile clientPrivateKeyFile
    ) {
        return grpcService.call(grpcReq,trustCertCollectionFile,clientCertChainFile,clientPrivateKeyFile);
    }

    @PostMapping("/query")
    public DeferredResult<ResponseEntity<List<DescriptorRes>>> queryDescriptor(@RequestPart("grpcReq") @Validated GrpcReq grpcReq
            , @RequestPart(name = "trustCertCollectionFile",required = false) MultipartFile trustCertCollectionFile
            , @RequestPart(name = "clientCertChainFile",required = false) MultipartFile clientCertChainFile
            , @RequestPart(name = "clientPrivateKeyFile",required = false) MultipartFile clientPrivateKeyFile
    ) {
        return grpcService.queryDescriptor(grpcReq,trustCertCollectionFile,clientCertChainFile,clientPrivateKeyFile);
    }
}
