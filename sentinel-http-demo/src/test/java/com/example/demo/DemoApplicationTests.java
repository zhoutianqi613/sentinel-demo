package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

@SpringBootTest
class DemoApplicationTests {

    @Test
    void testHttpController() {

        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(100);
        for (int i = 0; ; i++) {
            final int ii = i;
            fixedThreadPool.execute(() -> {
                RestTemplate restTemplate = new RestTemplate();
                String url = "http://localhost:8081/http/hello/1";
                String forObject = restTemplate.getForObject(url, String.class);
                System.currentTimeMillis();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");//设置日期格式
                System.out.println(String.format("[%s]response:%s", df.format(new Date()), forObject));
            });
        }
    }

}
