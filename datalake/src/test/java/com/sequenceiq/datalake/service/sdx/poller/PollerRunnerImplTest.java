package com.sequenceiq.datalake.service.sdx.poller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;

@ExtendWith(MockitoExtension.class)
public class PollerRunnerImplTest {

    private final PollerRunnerImpl underTest = new PollerRunnerImpl();

    @Test
    void testRun() {
        PollingConfig pollingConfig = new PollingConfig(1L, TimeUnit.MINUTES, 2L, TimeUnit.HOURS);
        Consumer<PollingConfig> pollingCommand = config -> {
        };
        SdxCluster sdxCluster = new SdxCluster();

        PollerRunnerResult result = underTest.run(pollingConfig, pollingCommand, "myProcess", sdxCluster);

        assertTrue(result.isSuccess());
        assertNull(result.getException());
        assertNull(result.getMessage());
    }

    @Test
    void testRunWhenUserBreakException() {
        PollingConfig pollingConfig = new PollingConfig(1L, TimeUnit.MINUTES, 2L, TimeUnit.HOURS);
        UserBreakException userBreakException = new UserBreakException("A userBreakException");
        Consumer<PollingConfig> pollingCommand = config -> {
            throw userBreakException;
        };
        SdxCluster sdxCluster = new SdxCluster();

        PollerRunnerResult result = underTest.run(pollingConfig, pollingCommand, "myProcess", sdxCluster);

        assertFalse(result.isSuccess());
        assertEquals(userBreakException, result.getException());
        assertEquals("myProcess poller exited before timeout. Cause: com.dyngr.exception.UserBreakException: A userBreakException", result.getMessage());
    }

    @Test
    void testRunWhenTimeout() {
        PollingConfig pollingConfig = new PollingConfig(1L, TimeUnit.MINUTES, 2L, TimeUnit.HOURS);
        PollerStoppedException pollerStoppedException = new PollerStoppedException("A pollerStoppedException");
        Consumer<PollingConfig> pollingCommand = config -> {
            throw pollerStoppedException;
        };
        SdxCluster sdxCluster = new SdxCluster();

        PollerRunnerResult result = underTest.run(pollingConfig, pollingCommand, "myProcess", sdxCluster);

        assertFalse(result.isSuccess());
        assertEquals("myProcess poller timed out after 2 minutes.", result.getMessage());
    }

    @Test
    void testRunWhenPollerException() {
        PollingConfig pollingConfig = new PollingConfig(1L, TimeUnit.MINUTES, 2L, TimeUnit.HOURS);
        PollerException pollerStoppedException = new PollerException("A pollerException");
        Consumer<PollingConfig> pollingCommand = config -> {
            throw pollerStoppedException;
        };
        SdxCluster sdxCluster = new SdxCluster();

        PollerRunnerResult result = underTest.run(pollingConfig, pollingCommand, "myProcess", sdxCluster);

        assertFalse(result.isSuccess());
        assertEquals(pollerStoppedException, result.getException());
        assertEquals("myProcess poller failed. Cause: com.dyngr.exception.PollerException: A pollerException", result.getMessage());
    }

    @Test
    void testRunWhenException() {
        PollingConfig pollingConfig = new PollingConfig(1L, TimeUnit.MINUTES, 2L, TimeUnit.HOURS);
        RuntimeException exception = new RuntimeException("A Exception");
        Consumer<PollingConfig> pollingCommand = config -> {
            throw exception;
        };
        SdxCluster sdxCluster = new SdxCluster();

        PollerRunnerResult result = underTest.run(pollingConfig, pollingCommand, "myProcess", sdxCluster);

        assertFalse(result.isSuccess());
        assertEquals(exception, result.getException());
        assertEquals("myProcess failed. Cause: java.lang.RuntimeException: A Exception", result.getMessage());
    }

}
