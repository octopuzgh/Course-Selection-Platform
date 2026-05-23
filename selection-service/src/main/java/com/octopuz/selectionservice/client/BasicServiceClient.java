package com.octopuz.selectionservice.client;

import com.octopuz.selectionservice.dto.SelectionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    public String getStudent(String studentNo) {
        try {
            return restTemplate.getForObject(basicServiceUrl + "/students/" + studentNo, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String getCourse(String courseNo) {
        try {
            return restTemplate.getForObject(basicServiceUrl + "/courses/" + courseNo, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasSelectedCourse(String studentNo, String courseNo) {
        try {
            String url = basicServiceUrl + "/selections/check?studentNo=" + studentNo + "&courseNo=" + courseNo;
            String response = restTemplate.getForObject(url, String.class);
            log.info("[DEBUG] hasSelectedCourse HTTP response: {}", response);
            if (response == null) {
                return false;
            }
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(response);
            return json.getBoolean("data") != null && json.getBoolean("data");
        } catch (Exception e) {
            log.error("[DEBUG] hasSelectedCourse HTTP failed: {}", e.getMessage());
            return false;
        }
    }

    public String getAllCourses() {
        try {
            return restTemplate.getForObject(basicServiceUrl + "/courses", String.class);
        } catch (Exception e) {
            log.error("获取课程列表失败", e);
            return null;
        }
    }

    public boolean submitSelection(String studentNo, String courseNo) {
        try {
            String url = basicServiceUrl + "/selections/submit";
            SelectionRequest request = new SelectionRequest();
            request.setStudentNo(studentNo);
            request.setCourseNo(courseNo);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Role", "ADMIN");

            HttpEntity<SelectionRequest> entity = new HttpEntity<>(request, headers);
            String result = restTemplate.postForObject(url, entity, String.class);
            log.debug("学生{}选课{}结果: {}", studentNo, courseNo, result);
            return result != null && result.contains("选课成功");
        } catch (Exception e) {
            log.error("学生{}选课{}失败", studentNo, courseNo, e);
            return false;
        }
    }

    public boolean dropSelection(String studentNo, String courseNo) {
        try {
            String url = basicServiceUrl + "/selections/drop";
            SelectionRequest request = new SelectionRequest();
            request.setStudentNo(studentNo);
            request.setCourseNo(courseNo);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Role", "ADMIN");

            HttpEntity<SelectionRequest> entity = new HttpEntity<>(request, headers);
            String result = restTemplate.postForObject(url, entity, String.class);
            log.debug("学生{}退课{}结果: {}", studentNo, courseNo, result);
            return result != null && result.contains("退课成功");
        } catch (Exception e) {
            log.error("学生{}退课{}失败", studentNo, courseNo, e);
            return false;
        }
    }
}
