package com.usst.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.course.entity.TeachingClass;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TeachingClassMapper extends BaseMapper<TeachingClass> {

    /**
     * 容量扣减的唯一写入口：条件 UPDATE 保证任何并发下都不超卖。
     */
    @Update("UPDATE teaching_class SET selected_count = selected_count + 1 "
            + "WHERE id = #{id} AND status = 'OPEN' AND selected_count < capacity")
    int increaseSelected(@Param("id") Long id);

    @Update("UPDATE teaching_class SET selected_count = selected_count - 1 "
            + "WHERE id = #{id} AND selected_count > 0")
    int decreaseSelected(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM teaching_class WHERE semester_id = #{semesterId} "
            + "AND status = 'OPEN' AND selected_count >= capacity")
    int countFullClasses(@Param("semesterId") Long semesterId);
}
