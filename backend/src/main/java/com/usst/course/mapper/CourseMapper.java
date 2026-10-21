package com.usst.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.course.entity.Course;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CourseMapper extends BaseMapper<Course> {

    @Select("SELECT id FROM course WHERE name LIKE CONCAT('%', #{keyword}, '%') OR code LIKE CONCAT('%', #{keyword}, '%')")
    List<Long> selectIdsByKeyword(@Param("keyword") String keyword);

    @Select("SELECT id FROM course WHERE college_id = #{collegeId}")
    List<Long> selectIdsByCollege(@Param("collegeId") Long collegeId);
}
