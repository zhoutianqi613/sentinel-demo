package com.example.demo.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.PathVariable;

public class BlockerHandler {

    public static String defaultBlockerHandler(@PathVariable String id, BlockException blockException) {
        System.out.println("run into defaultBlockerHandler() ...");
        System.out.println(blockException.getClass());
        //return null;
        blockException.printStackTrace();
        //return fallForBack2(id, blockException);
        return "ok";
    }
}
