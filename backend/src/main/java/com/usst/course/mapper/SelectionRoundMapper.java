package com.usst.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.course.entity.SelectionRound;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SelectionRoundMapper extends BaseMapper<SelectionRound> {

    @Select("SELECT * FROM selection_round WHERE semester_id = #{semesterId} AND status = 'OPEN' "
            + "AND NOW() BETWEEN start_time AND end_time ORDER BY round_no LIMIT 1")
    SelectionRound selectCurrentOpen(@Param("semesterId") Long semesterId);

    @Select("SELECT * FROM selection_round WHERE semester_id = #{semesterId} AND round_no = #{roundNo}")
    SelectionRound selectByRoundNo(@Param("semesterId") Long semesterId, @Param("roundNo") Integer roundNo);
}
