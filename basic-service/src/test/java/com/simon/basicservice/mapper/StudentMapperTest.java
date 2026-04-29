package com.simon.basicservice.mapper;

import com.simon.basicservice.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class StudentMapperTest {
    @Autowired
    StudentMapper studentMapper;
    @Test
    public void test() {
        Student student = new Student();
        student.setName("octopuz");
        student.setStudentNo("202406830");
        student.setMajor("数据科学与大数据技术");
        student.setGrade(2024);
        int insert = studentMapper.insert(student);
        System.out.println(insert);
    }

}