package com.octopuz.selectionservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.octopuz.selectionservice.service.interf.LockService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockServiceImpl implements LockService {

    @Autowired
    private RedissonClient redissonClient;
    private final Map<String, RLock> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            lockMap.put(lockKey, lock);
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                lockMap.remove(lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = lockMap.get(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            lockMap.remove(lockKey);
        }
    }

    @Override
    public void lock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(lockKey);
        lockMap.put(lockKey, lock);
        lock.lock(leaseTime, timeUnit);
    }

    @Override
    public boolean isLocked(String lockKey) {
        RLock lock = lockMap.get(lockKey);
        return lock != null && lock.isLocked();
    }
}
