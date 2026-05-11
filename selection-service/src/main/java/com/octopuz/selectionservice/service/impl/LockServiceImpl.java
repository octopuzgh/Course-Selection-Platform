package com.octopuz.selectionservice.service.impl;

import com.octopuz.selectionservice.service.interf.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockServiceImpl implements LockService {

    private final RedissonClient redissonClient;

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            // Deleted:lockMap.put(lockKey, lock);
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            // Deleted:if (!acquired) {
            // Deleted:    lockMap.remove(lockKey);
            // Deleted:}
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        // Deleted:RLock lock = lockMap.get(lockKey);
        RLock lock = redissonClient.getLock(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            // Deleted:lockMap.remove(lockKey);
        }
    }
}
