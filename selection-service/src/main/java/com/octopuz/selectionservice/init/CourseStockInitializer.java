package com.octopuz.selectionservice.init;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.octopuz.selectionservice.client.BasicServiceClient;
import com.octopuz.selectionservice.service.interf.RankingService;
import com.octopuz.selectionservice.service.interf.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseStockInitializer implements CommandLineRunner {

    @Autowired
    private StockService stockService;
    @Autowired
    private RankingService rankingService;
    @Autowired
    private BasicServiceClient basicServiceClient;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化课程库存到Redis...");

        try {
            String coursesJson = basicServiceClient.getAllCourses();
            if (coursesJson != null) {
                JSONArray courses = JSON.parseArray(coursesJson);
                for (int i = 0; i < courses.size(); i++) {
                    JSONObject course = courses.getJSONObject(i);
                    String courseNo = course.getString("course_no");
                    Integer totalCapacity = course.getInteger("total_capacity");
                    Integer remaining = course.getInteger("remaining");

                    stockService.setStock(courseNo, totalCapacity, remaining);
                    rankingService.initCourseRanking(courseNo, remaining);
                    log.info("课程{}库存初始化完成，总量: {}, 剩余: {}", courseNo, totalCapacity, remaining);
                }
            }
            log.info("课程库存初始化完成");
        } catch (Exception e) {
            log.error("课程库存初始化失败", e);
        }
    }
}
