package com.sequenceiq.environment.environment.service.sdx;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.exception.SdxOperationFailedException;

@ExtendWith(MockitoExtension.class)
class SdxUpgradeCcmPollerServiceTest {

    private static final String CRN = "crn";

    private static final long ENV_ID = 123L;

    @Mock
    private SdxPollerProvider pollerProvider;

    @InjectMocks
    private SdxUpgradeCcmPollerService underTest;

    @Test
    void waitForUpgradeCcm() {
        ReflectionTestUtils.setField(underTest, "attempt", 1);
        ReflectionTestUtils.setField(underTest, "sleeptime", 1);
        AttemptResult<Void> attemptResult = AttemptResults.justFinish();
        when(pollerProvider.upgradeCcmPoller(ENV_ID, CRN)).thenReturn(attemptResult);
        underTest.waitForUpgradeCcm(ENV_ID, CRN);
        verify(pollerProvider).upgradeCcmPoller(ENV_ID, CRN);
    }

    @Test
    void pollerException() {
        ReflectionTestUtils.setField(underTest, "attempt", 1);
        ReflectionTestUtils.setField(underTest, "sleeptime", 1);
        when(pollerProvider.upgradeCcmPoller(ENV_ID, CRN)).thenThrow(new IllegalStateException("error"));
        assertThatThrownBy(() -> underTest.waitForUpgradeCcm(ENV_ID, CRN))
                .isInstanceOf(SdxOperationFailedException.class);
        verify(pollerProvider).upgradeCcmPoller(ENV_ID, CRN);
    }
}
