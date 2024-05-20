package com.migu.gu.biz.res;

import com.migu.gu.biz.dto.GrpcField;
import lombok.Data;

import java.util.List;

/**
 * @Author kkmigu
 *
 * @Description
 */
@Data
public class DescriptorRes {
    private String method;
    private String message;
    private List<GrpcField> fieldList;
}
