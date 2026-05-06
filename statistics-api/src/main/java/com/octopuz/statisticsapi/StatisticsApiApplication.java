package com.octopuz.statisticsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StatisticsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatisticsApiApplication.class, args);
    }
}
