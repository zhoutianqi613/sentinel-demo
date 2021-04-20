/*
 * Copyright 1999-2020 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.demo.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.demo.handler.BlockerHandler;
import com.example.demo.handler.FallbackHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HttpController {

    @Autowired
    public ApplicationContext applicationContext;

    @RequestMapping("/http/hello")
    @SentinelResource(value = "hello", fallback = "fallForBack2", fallbackClass = FallbackHandler.class)
    public String hello() {
        System.out.println("run into hello() ...");
        return "Welcome Back!";
    }

    @RequestMapping("/http/hello/{id}")
    @SentinelResource(value = "hello1"
            , blockHandler = "defaultBlockerHandler", blockHandlerClass = BlockerHandler.class
            , fallback = "fallForBack2", fallbackClass = FallbackHandler.class)
    public String hello(@PathVariable String id) {
        System.out.println("run into hello(String id) ...");
        try {
            Thread.sleep(System.currentTimeMillis() % 300 + 200);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
        return "Welcome Back! " + id;
    }
}