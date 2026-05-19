package com.octopuz.statisticsservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class BasicServiceClient {

    private final RestTemplate restTemplate;

    @Value("${basic-service.url}")
    private String basicServiceUrl;

    public BasicServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    public String getCourse(String courseNo) {
        try {
            return restTemplate.getForObject(basicServiceUrl + "/courses/" + courseNo, String.class);
        } catch (Exception e) {
            log.error("获取课程{}信息失败", courseNo, e);
            return null;
        }
    }
}