package com.octopuz.selectionservice.client;

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

    /**
     * 查询学生信息
     */
    public String getStudent(String studentNo) {
        try {
            return restTemplate.getForObject(basicServiceUrl + "/students/" + studentNo, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询课程信息
     */
    public String getCourse(String courseNo) {
        try {
            return restTemplate.getForObject(basicServiceUrl + "/courses/" + courseNo, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 写入选课记录
     */
    public boolean saveSelectionRecord(String studentNo, String courseNo) {
        try {
            String url = basicServiceUrl + "/selections";
            String body = "{\"studentNo\":\"" + studentNo + "\",\"courseNo\":\"" + courseNo + "\"}";
            restTemplate.postForObject(url, body, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查学生是否已选该课程
     */
    public boolean hasSelectedCourse(String studentNo, String courseNo) {
        try {
            String url = basicServiceUrl + "/selections/check?studentNo=" + studentNo + "&courseNo=" + courseNo;
            Boolean result = restTemplate.getForObject(url, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取所有课程列表
     */
    public String getAllCourses() {
        try {
            return restTemplate.getForObject(basicServiceUrl + "/courses", String.class);
        } catch (Exception e) {
            log.error("获取课程列表失败", e);
            return null;
        }
    }
}
