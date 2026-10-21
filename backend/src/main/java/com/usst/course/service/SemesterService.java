package com.usst.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.course.common.BizException;
import com.usst.course.common.ErrorCode;
import com.usst.course.dto.Api;
import com.usst.course.entity.Semester;
import com.usst.course.entity.SelectionRound;
import com.usst.course.mapper.SelectionRoundMapper;
import com.usst.course.mapper.SemesterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SemesterMapper semesterMapper;
    private final SelectionRoundMapper roundMapper;

    public Semester current() {
        Semester s = semesterMapper.selectOne(
                Wrappers.<Semester>lambdaQuery().orderByDesc(Semester::getId).last("LIMIT 1"));
        if (s == null) {
            throw new BizException(ErrorCode.SERVER_ERROR.getCode(), "尚未配置学期，请先初始化数据");
        }
        return s;
    }

    public SelectionRound currentRound(Long semesterId) {
        return roundMapper.selectCurrentOpen(semesterId);
    }

    public Api.SemesterInfo info() {
        Semester s = current();
        SelectionRound round = roundMapper.selectCurrentOpen(s.getId());
        Api.RoundInfo roundInfo = round == null ? null : toRoundInfo(round);
        return new Api.SemesterInfo(s.getId(), s.getYear(), s.getTerm(), s.getName(), s.getStartDate(),
                s.getTotalWeeks(), s.getCurrentWeek(), s.getLocked(),
                LocalDateTime.now().format(FMT), roundInfo);
    }

    public Api.RoundInfo toRoundInfo(SelectionRound round) {
        long remaining = 0L;
        if (round.getEndTime() != null) {
            remaining = Math.max(0L, Duration.between(LocalDateTime.now(), round.getEndTime()).getSeconds());
        }
        return new Api.RoundInfo(round.getId(), round.getRoundNo(), round.getName(), round.getStatus(),
                round.getStartTime(), round.getEndTime(), round.getAllowDrop(), remaining);
    }
}
