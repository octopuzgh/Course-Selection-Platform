package com.octopuz.statisticsservice.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spark")
public class SparkProperties {

    private String appName = "StatisticsOffline";
    private String master = "local[*]";
    private String driverMemory = "512m";
    private boolean sqlAdaptiveEnabled = true;
    private boolean sqlAdaptiveCoalescePartitionsEnabled = true;
    private boolean enabled = false;
}
