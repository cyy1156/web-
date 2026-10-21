package com.usst.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口出入参集合。统一用 record，避免为每个请求建一个文件。
 */
public final class Api {

    private Api() {
    }

    public record LoginReq(@NotBlank(message = "请输入学号或工号") String username,
                           @NotBlank(message = "请输入密码") String password) {
    }

    public record LoginRes(String token, String role, String name, Long userId, Long studentId,
                           String studentNo, Integer grade, String majorName, String className) {
    }

    public record MeRes(String role, String name, Long studentId, String studentNo,
                        Integer grade, String majorName, String className) {
    }

    public record RoundInfo(Long id, Integer roundNo, String name, String status,
                            LocalDateTime startTime, LocalDateTime endTime,
                            Integer allowDrop, Long remainingSeconds) {
    }

    public record SemesterInfo(Long id, Integer year, Integer term, String name, LocalDate startDate,
                               Integer totalWeeks, Integer currentWeek, Integer locked,
                               String serverTime, RoundInfo round) {
    }

    public record EnrollReq(@NotNull(message = "请选择教学班") Long teachingClassId) {
    }

    public record EnrollRes(Long enrollmentId, BigDecimal totalCredits) {
    }

    public record CreditStatus(String name, String studentNo, String majorName, String className,
                               BigDecimal selectedCredits, BigDecimal passedCredits,
                               BigDecimal planCredits, BigDecimal minCredits, BigDecimal maxCredits) {
    }

    public record TimeSlot(Integer dayOfWeek, Integer startSection, Integer endSection, String weekType) {
    }

    public record ClassSaveReq(Long courseId, String classNo, Long teacherId, Integer capacity,
                               String campus, String classroom, List<TimeSlot> timeSlots) {
    }

    public record RoundUpdateReq(String status, LocalDateTime startTime, LocalDateTime endTime, Integer allowDrop) {
    }

    public record PlanCourse(Long courseId, String courseName, BigDecimal credits, String requiredType,
                             String status, Integer score) {
    }

    public record PlanTerm(Integer termNo, List<PlanCourse> courses) {
    }

    public record PlanRes(List<PlanTerm> terms, BigDecimal passedCredits, BigDecimal earningCredits,
                          BigDecimal requiredCredits) {
    }

    public record RemainItem(Long id, Integer remaining, Integer selectedCount, String status) {
    }

    public record TimetableItem(Long enrollmentId, Long courseId, String courseName, String teacherName,
                                String classroom, String courseType, Integer colorKey, List<TimeVO> times) {
    }

    public record TimetableRes(List<TimetableItem> items, Long busyBitmap) {
    }
}
