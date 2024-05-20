package com.migu.gu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Author kkmigu
 *
 * @Description
 */
@Controller
public class ViewController {
    @GetMapping(value = {"/","/index"})
    public String index() {
        return "index";
    }
}
