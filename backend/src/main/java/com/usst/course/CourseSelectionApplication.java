package com.usst.course;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 选课系统启动类。
 *
 * <p>教学演示项目，对标上海理工大学《全日制本科生选课办法》，非官方系统。
 */
@SpringBootApplication
@MapperScan("com.usst.course.mapper")
public class CourseSelectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseSelectionApplication.class, args);
    }
}
