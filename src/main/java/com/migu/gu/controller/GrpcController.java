package com.migu.gu.controller;

import com.migu.gu.biz.req.GrpcReq;
import com.migu.gu.biz.res.DescriptorRes;
import com.migu.gu.grpc.service.GrpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author kkmigu
 *
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
    public DeferredResult<ResponseEntity<Object>> call(@RequestBody @Validated GrpcReq grpcReq) {
        return grpcService.call(grpcReq);
    }

    @PostMapping("/query")
    public DeferredResult<ResponseEntity<List<DescriptorRes>>> queryDescriptor(@RequestBody @Validated GrpcReq grpcReq) {
        return grpcService.queryDescriptor(grpcReq);
    }
}
