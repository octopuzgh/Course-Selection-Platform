package com.octopuz.selectionservice.service.interf;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁服务接口
 */
public interface LockService {

    /**
     * 尝试获取锁
     * @param lockKey 锁Key
     * @param waitTime 等待时间
     * @param leaseTime 持有时间
     * @param timeUnit 时间单位
     * @return true表示获取成功
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit);

    /**
     * 释放锁
     * @param lockKey 锁Key
     */
    void unlock(String lockKey);
}
