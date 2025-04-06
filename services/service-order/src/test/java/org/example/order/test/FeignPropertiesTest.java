package org.example.order.test;

import lombok.extern.slf4j.Slf4j;
import org.example.order.feign.client.DirectFeignClient2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class FeignPropertiesTest {

    @Autowired
    private DirectFeignClient2 directFeignClient2;

    @Test
    public void testFeignClientProperties() {
        String word = "word";
        String result = directFeignClient2.queryWord(word);
        log.info("word: {}, result: \n{}", word, result);
    }
}
