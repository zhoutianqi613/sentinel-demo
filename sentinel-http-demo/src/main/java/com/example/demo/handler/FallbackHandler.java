package com.example.demo.handler;

import org.springframework.web.bind.annotation.PathVariable;

public class FallbackHandler {
    public static String fallForBack2(@PathVariable String id, Throwable throwable) {
        System.out.println("run into fallForBack2() ...");
        //System.out.println(throwable.getMessage());
        //throwable.printStackTrace();
        return "该服务已经被限流...";
    }
}
