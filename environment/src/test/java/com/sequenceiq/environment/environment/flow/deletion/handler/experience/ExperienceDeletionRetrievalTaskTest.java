package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

class ExperienceDeletionRetrievalTaskTest {

    private static final String TEST_ENV_NAME = "someEnvName";

    private static final String TEST_ENV_CRN = "someEnvCrn";

    private static final String TEST_ACCOUNT_ID = "someAccountId";

    private ExperiencePollerObject testExperiencePollerObject;

    @Mock
    private ExperienceConnectorService mockExperienceConnectorService;

    private ExperienceDeletionRetrievalTask underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new ExperienceDeletionRetrievalTask(mockExperienceConnectorService);
        testExperiencePollerObject = new ExperiencePollerObject(TEST_ENV_CRN, TEST_ENV_NAME, TEST_ACCOUNT_ID);
    }

    @Test
    void testExperienceRetryingIntervalShouldBeTheExpected() {
        assertEquals(5000, ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_INTERVAL);
    }

    @Test
    void testExperienceRetryingCountShouldBeTheExpected() {
        assertEquals(900, ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_COUNT);
    }

    @Test
    void testCheckStatusWhenExperienceConnectorServiceTellsThatTheEnvHasNoConnectedExperienceThenTrueShouldReturn() {
        when(mockExperienceConnectorService.getConnectedExperienceCount(any(EnvironmentExperienceDto.class))).thenReturn(0);

        assertTrue(underTest.checkStatus(testExperiencePollerObject));
    }

    @Test
    void testCheckStatusWhenExperienceConnectorServiceTellsThatTheEnvHasConnectedExperiencesThenFalseShouldReturn() {
        when(mockExperienceConnectorService.getConnectedExperienceCount(any(EnvironmentExperienceDto.class))).thenReturn(1);

        assertFalse(underTest.checkStatus(testExperiencePollerObject));
    }

    @Test
    void testHandleTimeoutShouldThrowCloudbreakServiceException() {
        CloudbreakServiceException expectedException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.handleTimeout(testExperiencePollerObject));

        assertNotNull(expectedException);
        assertEquals("Checking Experience operation has timed out!", expectedException.getMessage());
    }

    @Test
    void testSuccessMessageShouldReturnTheExpectedContent() {
        assertEquals("Experience deletion was successful!", underTest.successMessage(testExperiencePollerObject));
    }

    @Test
    void testExitPollingWhenExperienceConnectorServiceTellsThatTheEnvHasNoConnectedExperienceThenTrueShouldReturn() {
        when(mockExperienceConnectorService.getConnectedExperienceCount(any(EnvironmentExperienceDto.class))).thenReturn(0);

        assertTrue(underTest.exitPolling(testExperiencePollerObject));
    }

    @Test
    void testExitPollingWhenExperienceConnectorServiceTellsThatTheEnvHasConnectedExperiencesThenFalseShouldReturn() {
        when(mockExperienceConnectorService.getConnectedExperienceCount(any(EnvironmentExperienceDto.class))).thenReturn(1);

        assertFalse(underTest.exitPolling(testExperiencePollerObject));
    }

}