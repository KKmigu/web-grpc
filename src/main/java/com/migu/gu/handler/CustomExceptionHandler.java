package com.migu.gu.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Author kkmigu
 *
 * @Description
 */
@Order(-1)
@RequiredArgsConstructor
@Slf4j
@ControllerAdvice
public class CustomExceptionHandler {

    /**
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseBody
    public String bizExceptionHandler(MethodArgumentNotValidException e, HttpServletResponse response) {
        log.error("Validation failed: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return ((BeanPropertyBindingResult) e.getBindingResult()).getAllErrors().get(0).getDefaultMessage();
    }
}
