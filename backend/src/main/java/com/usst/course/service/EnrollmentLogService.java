package com.usst.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.course.entity.EnrollmentLog;
import com.usst.course.mapper.EnrollmentLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentLogService {

    private final EnrollmentLogMapper logMapper;

    /**
     * 失败留痕。必须独立事务，否则会被主流程回滚掉。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordFailure(Long studentId, Long classId, String action, int code, String message,
                              String idemKey, long costMs) {
        EnrollmentLog entry = new EnrollmentLog();
        entry.setStudentId(studentId);
        entry.setTeachingClassId(classId);
        entry.setAction(action);
        entry.setResultCode(code);
        entry.setMessage(message);
        entry.setIdempotencyKey(idemKey);
        entry.setCostMs((int) costMs);
        entry.setCreatedAt(LocalDateTime.now());
        try {
            logMapper.insert(entry);
        } catch (DuplicateKeyException ignored) {
            // 同一幂等键并发写入，忽略
        } catch (Exception e) {
            log.warn("写入选课日志失败: {}", e.getMessage());
        }
    }

    public EnrollmentLog findByIdemKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return logMapper.selectOne(Wrappers.<EnrollmentLog>lambdaQuery()
                .eq(EnrollmentLog::getIdempotencyKey, key).last("LIMIT 1"));
    }
}
