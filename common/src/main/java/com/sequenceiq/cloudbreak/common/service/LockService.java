package com.sequenceiq.cloudbreak.common.service;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LockService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockService.class);

    @Inject
    private JdbcTemplate jdbcTemplate;

    public void lockAndRunIfLockWasSuccessful(Runnable runnable, LockNumber lockNumber) {
        LOGGER.info("Try to get PostgreSQL advisory lock with lock number: {}", lockNumber);
        boolean lockSuccess = false;
        try {
            lockSuccess = lock(lockNumber, jdbcTemplate);
            if (lockSuccess) {
                LOGGER.info("PostgreSQL advisory lock was successful with lock number: {}", lockNumber);
                runnable.run();
            } else {
                LOGGER.warn("PostgreSQL advisory lock was unsuccessful with lock number: {}", lockNumber);
            }
        } finally {
            if (lockSuccess) {
                unlock(lockNumber, jdbcTemplate);
            }
        }
    }

    private void unlock(LockNumber lockNumber, JdbcTemplate jdbcTemplate) {
        Boolean unlocked = jdbcTemplate.queryForObject("SELECT pg_advisory_unlock(" + lockNumber.getLockNumber() + ")", Boolean.class);
        if (Boolean.FALSE.equals(unlocked)) {
            LOGGER.error("Unable to release PostgreSQL advisory lock with lock number: {}", lockNumber);
        } else {
            LOGGER.info("PostgreSQL advisory lock was released with lock number: {}", lockNumber);
        }
    }

    private boolean lock(LockNumber lockNumber, JdbcTemplate jdbcTemplate) {
        List<Boolean> results = jdbcTemplate.query("SELECT pg_try_advisory_lock(" + lockNumber.getLockNumber() + ")",
                (rs, rowNum) -> rs.getBoolean("pg_try_advisory_lock"));
        return results.size() == 1 && results.get(0);
    }

}
