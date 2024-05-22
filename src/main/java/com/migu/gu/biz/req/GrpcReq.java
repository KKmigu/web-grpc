package com.migu.gu.biz.req;

import com.migu.gu.biz.dto.GrpcField;
import com.migu.gu.biz.enums.TransportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.List;

/**
 * @Author kkmigu
 *
 * @Description
 */
@Data
public class GrpcReq implements Serializable {
    @NotBlank(message = "address can not be empty")
    private String address;
    @NotNull(message = "port can not be empty")
    @Range(min = 1, max = 65535, message = "port must be between 1 and 65535")
    private Integer port;
    @NotBlank(message = "fullService can not be empty")
    private String fullService;
    private String method;
    private String message;
    private List<GrpcField> fieldList;
    @NotNull(message = "transportType can not be empty")
    private TransportType transportType;

    private ProxyReq proxyReq;
}
