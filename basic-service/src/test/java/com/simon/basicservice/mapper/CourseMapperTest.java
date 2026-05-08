package com.simon.basicservice.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootTest
class CourseMapperTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CourseMapper courseMapper;

    @Test
    public void testDatabaseConnection() {

        long count = courseMapper.selectCount(null);
        System.out.println("数据库连接成功！用户表记录数: " + count);
    }

//        try (Connection connection = dataSource.getConnection()) {
//            System.out.println("✅ 数据库连接成功！");
//            System.out.println("数据库URL: " + connection.getMetaData().getURL());
//            System.out.println("数据库用户: " + connection.getMetaData().getUserName());
//            System.out.println("数据库产品: " + connection.getMetaData().getDatabaseProductName());
//            System.out.println("数据库版本: " + connection.getMetaData().getDatabaseProductVersion());
//        } catch (SQLException e) {
//            System.err.println("数据库连接失败！");
//            System.err.println("错误信息: " + e.getMessage());
//            e.printStackTrace();
//            throw new RuntimeException("数据库连接测试失败", e);
//        }
//    }
}
