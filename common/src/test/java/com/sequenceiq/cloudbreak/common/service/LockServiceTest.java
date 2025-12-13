package com.sequenceiq.cloudbreak.common.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private LockService lockService;

    @Test
    public void lockAndRunIfLockWasSuccessfulTest() {
        AtomicBoolean lockHappened = new AtomicBoolean(false);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(true));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);
        lockService.lockAndRunIfLockWasSuccessful(() -> lockHappened.set(true), LockNumber.QUARTZ);
        verify(jdbcTemplate, times(1)).queryForObject(eq("SELECT pg_advisory_unlock(647)"), eq(Boolean.class));
        verify(jdbcTemplate, times(1)).query(eq("SELECT pg_try_advisory_lock(647)"), any(RowMapper.class));
        assertTrue(lockHappened.get());
    }

    @Test
    public void lockAndRunIfLockWasSuccessfulButReleaseWasUnsuccessfulTest() {
        AtomicBoolean lockHappened = new AtomicBoolean(false);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(true));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(false);
        lockService.lockAndRunIfLockWasSuccessful(() -> lockHappened.set(true), LockNumber.QUARTZ);
        assertTrue(lockHappened.get());
    }

    @Test
    public void lockAndRunIfLockWasUnSuccessfulTest() {
        AtomicBoolean lockHappened = new AtomicBoolean(false);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(false));
        lockService.lockAndRunIfLockWasSuccessful(() -> lockHappened.set(true), LockNumber.QUARTZ);
        verify(jdbcTemplate, times(0)).queryForObject(anyString(), eq(Boolean.class));
        assertFalse(lockHappened.get());
    }

    @Test
    public void lockAndRunIfLockWasSuccessfulButRunnableThrowExecptionTest() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(true));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);
        assertThrows(RuntimeException.class, () -> lockService.lockAndRunIfLockWasSuccessful(() -> {
            throw new RuntimeException("runnable failed");
        }, LockNumber.QUARTZ));
        verify(jdbcTemplate, times(1)).queryForObject(eq("SELECT pg_advisory_unlock(647)"), eq(Boolean.class));
        verify(jdbcTemplate, times(1)).query(eq("SELECT pg_try_advisory_lock(647)"), any(RowMapper.class));
    }
}