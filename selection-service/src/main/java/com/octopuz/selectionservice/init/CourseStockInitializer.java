package com.octopuz.selectionservice.init;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.octopuz.selectionservice.client.BasicServiceClient;
import com.octopuz.selectionservice.service.interf.RankingService;
import com.octopuz.selectionservice.service.interf.StockService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${REDIS_HOST:localhost}")
    private String redisHost;

    @Value("${REDIS_PORT:6379}")
    private int redisPort;

    @Value("${REDIS_DATABASE:0}")
    private int redisDatabase;

    @PostConstruct
    public void init() {
        log.info("Redis配置 - Host: {}, Port: {}, Database: {}", redisHost, redisPort, redisDatabase);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化课程库存到Redis (数据库: {})...", redisDatabase);

        try {
            String coursesJson = basicServiceClient.getAllCourses();

            if (coursesJson == null || coursesJson.trim().isEmpty()) {
                log.warn("未获取到课程数据，跳过初始化");
                return;
            }

            log.debug("获取到的课程数据: {}", coursesJson);

            JSONObject response = JSON.parseObject(coursesJson);
            Integer code = response.getInteger("code");
            if (code == null || code != 200) {
                log.error("接口返回错误，code: {}", code);
                return;
            }

            JSONArray courses = response.getJSONArray("data");
            if (courses == null || courses.isEmpty()) {
                log.warn("课程列表为空");
                return;
            }

            log.info("共获取到 {} 个课程", courses.size());

            for (int i = 0; i < courses.size(); i++) {
                JSONObject course = courses.getJSONObject(i);
                String courseNo = course.getString("courseNo");
                Integer totalCapacity = course.getInteger("totalCapacity");
                Integer remaining = course.getInteger("remaining");

                if (courseNo != null && totalCapacity != null && remaining != null) {
                    stockService.setStock(courseNo, totalCapacity, remaining);
                    rankingService.initCourseRanking(courseNo, remaining);
                    log.info("课程{}库存初始化完成，总量: {}, 剩余: {}", courseNo, totalCapacity, remaining);
                } else {
                    log.warn("课程数据不完整: {}", course);
                }
            }
            log.info("课程库存初始化完成 (Redis数据库: {})", redisDatabase);
        } catch (Exception e) {
            log.error("课程库存初始化失败", e);
        }
    }
}
