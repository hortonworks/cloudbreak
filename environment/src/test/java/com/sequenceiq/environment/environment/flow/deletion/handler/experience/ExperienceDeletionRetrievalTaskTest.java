package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.ExperienceCluster;
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
    void testHandleTimeoutShouldNotThrow() {
        underTest.handleTimeout(testExperiencePollerObject);
    }

    @Test
    void testHandleTimeoutShouldNotThrowCloudbreakServiceException() {
        underTest.handleException(new Exception("should passthrough"));
    }

    @Test
    void testSuccessMessageShouldReturnTheExpectedContent() {
        assertEquals("Experience deletion was successful!", underTest.successMessage(testExperiencePollerObject));
    }

    @Test
    void testExitPollingWhenExperienceConnectorServiceReturnsOneFailedThenTrueShouldReturn() {
        ExperienceCluster failedCluster = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("availableCluster1")
                .withStatus("DELETE_FAILED")
                .build();
        ExperienceCluster nonFailedCluster = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("availableCluster2")
                .withStatus("AVAILABLE")
                .build();

        when(mockExperienceConnectorService.getConnectedExperiences(any(EnvironmentExperienceDto.class))).thenReturn(Set.of(failedCluster, nonFailedCluster));

        assertTrue(underTest.exitPolling(testExperiencePollerObject));
    }

    @Test
    void testExitPollingWhenExperienceConnectorServiceReturnsAvailableThenFalseShouldReturn() {
        ExperienceCluster nonFailedCluster1 = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("availableCluster1")
                .withStatus("AVAILABLE")
                .build();
        ExperienceCluster nonFailedCluster2 = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("availableCluster2")
                .withStatus("AVAILABLE")
                .build();

        when(mockExperienceConnectorService.getConnectedExperiences(any(EnvironmentExperienceDto.class)))
                .thenReturn(Set.of(nonFailedCluster1, nonFailedCluster2));

        assertFalse(underTest.exitPolling(testExperiencePollerObject));
    }
}
