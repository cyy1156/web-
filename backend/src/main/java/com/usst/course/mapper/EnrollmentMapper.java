package com.usst.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.course.entity.Enrollment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

public interface EnrollmentMapper extends BaseMapper<Enrollment> {

    @Select("SELECT COALESCE(SUM(credits), 0) FROM enrollment "
            + "WHERE student_id = #{studentId} AND semester_id = #{semesterId} AND status = 'SELECTED'")
    BigDecimal sumSelectedCredits(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    @Select("SELECT teaching_class_id FROM enrollment "
            + "WHERE student_id = #{studentId} AND semester_id = #{semesterId} AND status = 'SELECTED'")
    List<Long> selectSelectedClassIds(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    /** 学生已选课程的星期+节次位图，冲突判定用 */
    @Select("SELECT BIT_OR(tc.time_bitmap) FROM enrollment e "
            + "JOIN teaching_class tc ON tc.id = e.teaching_class_id "
            + "WHERE e.student_id = #{studentId} AND e.semester_id = #{semesterId} AND e.status = 'SELECTED'")
    Long selectBusyBitmap(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    @Select("SELECT COUNT(*) FROM enrollment WHERE student_id = #{studentId} AND semester_id = #{semesterId} "
            + "AND course_id = #{courseId} AND status = 'SELECTED'")
    int countSameCourse(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId,
                        @Param("courseId") Long courseId);

    @Select("SELECT COUNT(*) FROM enrollment WHERE semester_id = #{semesterId} AND status = 'SELECTED'")
    long countBySemester(@Param("semesterId") Long semesterId);

    @Select("SELECT COUNT(DISTINCT student_id) FROM enrollment WHERE semester_id = #{semesterId} AND status = 'SELECTED'")
    long countStudents(@Param("semesterId") Long semesterId);
}
