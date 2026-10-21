package com.usst.course.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 管理端统计，直接走聚合 SQL。
 */
public interface StatsMapper {

    @Select("SELECT col.name AS collegeName, SUM(tc.capacity) AS capacity, SUM(tc.selected_count) AS enrolled, "
            + "COUNT(tc.id) AS classCount "
            + "FROM teaching_class tc "
            + "JOIN course c ON c.id = tc.course_id "
            + "JOIN college col ON col.id = c.college_id "
            + "WHERE tc.semester_id = #{semesterId} AND tc.status = 'OPEN' "
            + "GROUP BY col.id, col.name ORDER BY enrolled DESC")
    List<Map<String, Object>> collegeRates(@Param("semesterId") Long semesterId);

    @Select("SELECT c.name AS courseName, SUM(tc.selected_count) AS selected, SUM(tc.capacity) AS capacity "
            + "FROM teaching_class tc JOIN course c ON c.id = tc.course_id "
            + "WHERE tc.semester_id = #{semesterId} AND tc.status = 'OPEN' "
            + "GROUP BY c.id, c.name ORDER BY selected DESC LIMIT 10")
    List<Map<String, Object>> hotCourses(@Param("semesterId") Long semesterId);

    @Select("SELECT AVG(cnt) FROM (SELECT student_id, COUNT(*) AS cnt FROM enrollment "
            + "WHERE semester_id = #{semesterId} AND status = 'SELECTED' GROUP BY student_id) t")
    Double avgCourses(@Param("semesterId") Long semesterId);
}
