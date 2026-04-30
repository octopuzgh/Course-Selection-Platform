package com.octopuz.selectionservice.service.interf;

/**
 * 库存服务接口
 */
public interface StockService {

    /**
     * 检查课程库存是否充足
     * @param courseNo 课程号
     * @return true表示库存充足
     */
    boolean checkStock(String courseNo);

    /**
     * 预扣库存
     * @param courseNo 课程号
     * @return true表示扣减成功
     */
    boolean deductStock(String courseNo);

    /**
     * 恢复库存
     * @param courseNo 课程号
     */
    void restoreStock(String courseNo);

    /**
     * 设置课程库存
     * @param courseNo 课程号
     * @param totalCapacity 总容量
     * @param remaining 剩余名额
     */
    void setStock(String courseNo, Integer totalCapacity, Integer remaining);

    /**
     * 获取当前库存
     * @param courseNo 课程号
     * @return 库存数量
     */
    Integer getRemaining(String courseNo);

    /**
     * 获取总容量
     * @param courseNo 课程号
     * @return 总容量
     */
    Integer getTotalCapacity(String courseNo);
}
