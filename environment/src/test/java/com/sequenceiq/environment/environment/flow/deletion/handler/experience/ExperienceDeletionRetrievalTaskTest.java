package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.ExperienceCluster;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

class ExperienceDeletionRetrievalTaskTest {

    private static final String TEST_ENV_NAME = "someEnvName";

    private static final String TEST_CLOUD_PLATFORM = "someCloudPlatform";

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
        testExperiencePollerObject = new ExperiencePollerObject(TEST_ENV_CRN, TEST_ENV_NAME, TEST_CLOUD_PLATFORM, TEST_ACCOUNT_ID);
    }

    @Test
    void testExperienceRetryingIntervalShouldBeTheExpected() {
        assertEquals(6000, ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_INTERVAL_IN_MILLISECONDS);
    }

    @Test
    void testExperienceRetryingCountShouldBeTheExpected() {
        assertEquals(1500, ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_COUNT);
    }

    @Test
    void testCheckStatusWhenExperienceConnectorServiceTellsThatTheEnvHasNoConnectedExperienceThenTrueShouldReturn() {
        when(mockExperienceConnectorService.getConnectedExperiences(any(EnvironmentExperienceDto.class)))
                .thenReturn(Set.of());

        assertTrue(underTest.checkStatus(testExperiencePollerObject));
    }

    @Test
    void testCheckStatusWhenExperienceConnectorServiceTellsThatTheEnvHasConnectedExperiencesThenFalseShouldReturn() {
        ExperienceCluster av1 = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("availableCluster1")
                .withStatus("AVAILABLE")
                .build();
        ExperienceCluster av2 = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("availableCluster2")
                .withStatus("AVAILABLE")
                .build();
        when(mockExperienceConnectorService.getConnectedExperiences(any(EnvironmentExperienceDto.class)))
                .thenReturn(Set.of(av1, av2));

        assertFalse(underTest.checkStatus(testExperiencePollerObject));
    }

    @Test
    void testCheckStatusWhenExperienceConnectorServiceTellsThatTheEnvHasConnectedExperiencesFailedWithStatusReasonToDeleteThenThrowException() {
        ExperienceCluster deleteFailed1 = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("deleteFailed1")
                .withStatusReason("Very bad thing")
                .withStatus("DELETE_FAILED")
                .build();
        ExperienceCluster av2 = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("availableCluster2")
                .withStatus("AVAILABLE")
                .build();
        when(mockExperienceConnectorService.getConnectedExperiences(any(EnvironmentExperienceDto.class)))
                .thenReturn(Set.of(deleteFailed1, av2));

        assertThatThrownBy(() -> underTest.checkStatus(testExperiencePollerObject))
                .isInstanceOf(ExperienceOperationFailedException.class)
                .hasMessage("Failed to delete deleteFailed1 experience, the problem was: Very bad thing");
    }

    @Test
    void testCheckStatusWhenExperienceConnectorServiceTellsThatTheEnvHasConnectedTwoExperiencesFailedWithStatusReasonToDeleteThenThrowException() {
        ExperienceCluster deleteFailed1 = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("deleteFailed1")
                .withStatusReason("Very bad thing")
                .withStatus("DELETE_FAILED")
                .build();
        ExperienceCluster deleteFailed2 = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("deleteFailed2")
                .withStatusReason("Very bad thing")
                .withStatus("DELETE_FAILED")
                .build();
        ExperienceCluster av2 = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("availableCluster2")
                .withStatus("AVAILABLE")
                .build();
        when(mockExperienceConnectorService.getConnectedExperiences(any(EnvironmentExperienceDto.class)))
                .thenReturn(Set.of(deleteFailed1, deleteFailed2, av2));

        assertThatThrownBy(() -> underTest.checkStatus(testExperiencePollerObject))
                .isInstanceOf(ExperienceOperationFailedException.class)
                .hasMessage("Failed to delete deleteFailed1 experience, the problem was: Very bad thing, " +
                        "Failed to delete deleteFailed2 experience, the problem was: Very bad thing");
    }

    @Test
    void testCheckStatusPollingWhenExperienceConnectorServiceReturnsOneFailedThenExceptionShouldThrow() {
        ExperienceCluster failedCluster = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("deleteFailed1")
                .withStatus("DELETE_FAILED")
                .build();
        ExperienceCluster nonFailedCluster = ExperienceCluster.builder()
                .withExperienceName("LIFTIE")
                .withName("availableCluster2")
                .withStatus("AVAILABLE")
                .build();

        when(mockExperienceConnectorService.getConnectedExperiences(any(EnvironmentExperienceDto.class))).thenReturn(Set.of(failedCluster, nonFailedCluster));

        assertThatThrownBy(() -> underTest.checkStatus(testExperiencePollerObject))
                .isInstanceOf(ExperienceOperationFailedException.class)
                .hasMessage("Failed to delete deleteFailed1 experience, the problem was: " +
                        "Could not identify the problem, please contact with our support team");
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
    void testExitPollingWhenExperienceConnectorServiceReturnsAvailableThenFalseShouldReturn() {
        when(mockExperienceConnectorService.getConnectedExperienceCount(any(EnvironmentExperienceDto.class)))
                .thenReturn(2);

        assertFalse(underTest.exitPolling(testExperiencePollerObject));
    }
}
