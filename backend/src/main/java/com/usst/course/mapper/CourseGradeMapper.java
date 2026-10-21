package com.usst.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.course.entity.CourseGrade;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

public interface CourseGradeMapper extends BaseMapper<CourseGrade> {

    @Select("SELECT COALESCE(SUM(c.credits), 0) FROM course_grade g "
            + "JOIN course c ON c.id = g.course_id "
            + "WHERE g.student_id = #{studentId} AND g.passed = 1")
    BigDecimal sumPassedCredits(@Param("studentId") Long studentId);

    /** 某门课的历史最高分，未修过返回 null */
    @Select("SELECT MAX(g.score) FROM course_grade g WHERE g.student_id = #{studentId} AND g.course_id = #{courseId}")
    Integer bestScore(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
