package com.octopuz.selectionservice.service.impl;

import com.octopuz.selectionservice.service.interf.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    private static final String STOCK_KEY_PREFIX = "course:stock:";
    private static final String CAPACITY_KEY_PREFIX = "course:capacity:";

    @Override
    public boolean checkStock(String courseNo) {
        String key = STOCK_KEY_PREFIX + courseNo;
        String stockStr = redisTemplate.opsForValue().get(key);
        return stockStr != null && Integer.parseInt(stockStr) > 0;
    }

    @Override
    public boolean deductStock(String courseNo) {
        String key = STOCK_KEY_PREFIX + courseNo;
        Long result = redisTemplate.opsForValue().decrement(key);
        if (result == null || result < 0) {
            if (result != null && result < 0) {
                redisTemplate.opsForValue().increment(key);
            }
            return false;
        }
        log.debug("课程{}库存扣减成功，剩余: {}", courseNo, result);
        return true;
    }

    @Override
    public void restoreStock(String courseNo) {
        String key = STOCK_KEY_PREFIX + courseNo;
        redisTemplate.opsForValue().increment(key);
        log.debug("课程{}库存恢复成功", courseNo);
    }

    @Override
    public void setStock(String courseNo, Integer totalCapacity, Integer remaining) {
        String stockKey = STOCK_KEY_PREFIX + courseNo;
        String capacityKey = CAPACITY_KEY_PREFIX + courseNo;
        redisTemplate.opsForValue().set(stockKey, String.valueOf(remaining));
        redisTemplate.opsForValue().set(capacityKey, String.valueOf(totalCapacity));
        log.debug("课程{}库存初始化完成，总量: {}, 剩余: {}", courseNo, totalCapacity, remaining);
    }

    @Override
    public Integer getRemaining(String courseNo) {
        String key = STOCK_KEY_PREFIX + courseNo;
        String stockStr = redisTemplate.opsForValue().get(key);
        return stockStr != null ? Integer.parseInt(stockStr) : 0;
    }

    @Override
    public Integer getTotalCapacity(String courseNo) {
        String key = CAPACITY_KEY_PREFIX + courseNo;
        String capacityStr = redisTemplate.opsForValue().get(key);
        return capacityStr != null ? Integer.parseInt(capacityStr) : 0;
    }
}
