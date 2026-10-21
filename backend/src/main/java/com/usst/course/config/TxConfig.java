package com.usst.course.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 显式事务模板：让"事务体"与"失败日志"严格分离，
 * 避免高并发下持锁等待新连接导致连接池耗尽。
 */
@Configuration
public class TxConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager manager) {
        return new TransactionTemplate(manager);
    }
}
