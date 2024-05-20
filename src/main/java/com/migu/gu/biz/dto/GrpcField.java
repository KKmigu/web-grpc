package com.migu.gu.biz.dto;

import lombok.Data;

import java.util.List;

/**
 * @Author kkmigu
 *
 * @Description
 */
@Data
public class GrpcField {
    private String name;
    private String type;
    private Object value;
    private List<GrpcField> childFields;
}
