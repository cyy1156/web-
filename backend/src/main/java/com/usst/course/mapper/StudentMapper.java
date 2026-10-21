package com.usst.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.course.entity.Student;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StudentMapper extends BaseMapper<Student> {

    @Select("SELECT m.name FROM major m WHERE m.id = #{majorId}")
    String selectMajorName(@Param("majorId") Long majorId);
}
