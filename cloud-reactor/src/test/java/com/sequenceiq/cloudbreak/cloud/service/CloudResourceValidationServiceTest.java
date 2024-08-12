package com.sequenceiq.cloudbreak.cloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;

@ExtendWith(MockitoExtension.class)
class CloudResourceValidationServiceTest {
    private static final String STATUS_REASON_ERROR = "myerror";

    private static final String STATUS_REASON_SUCCESS = "all good";

    private static final long PRIVATE_ID_1 = 78L;

    private static final long PRIVATE_ID_2 = 56L;

    @InjectMocks
    private CloudResourceValidationService underTest;

    @Test
    void testValidateResourcesStateWhenNullPollerResult() throws Exception {
        IllegalStateException actualException = assertThrows(IllegalStateException.class,
                () -> underTest.validateResourcesState(mock(CloudContext.class), null));

        assertThat(actualException).isInstanceOf(IllegalStateException.class);
        assertThat(actualException).hasMessageStartingWith("ResourcesStatePollerResult is null, cannot check deploy status of database stack for ");
    }

    @Test
    void testValidateResourcesStateWhenNullPollerResultResults() throws Exception {
        ResourcesStatePollerResult resourcesStatePollerResult = mock(ResourcesStatePollerResult.class);
        when(resourcesStatePollerResult.getResults()).thenReturn(null);

        IllegalStateException actualException = assertThrows(IllegalStateException.class,
                () -> underTest.validateResourcesState(mock(CloudContext.class), resourcesStatePollerResult));

        assertThat(actualException).isInstanceOf(IllegalStateException.class);
        assertThat(actualException).hasMessageStartingWith("ResourcesStatePollerResult.results is null, cannot check deploy status of database stack for ");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("doAcceptTestWhenFailureBadPollerResultDataProvider")
    void testValidateResourcesStateSingleResource(String testCaseName, ResourceStatus resourceStatus) {
        ResourcesStatePollerResult resourcesStatePollerResult = mock(ResourcesStatePollerResult.class);
        CloudResourceStatus cloudResourceStatusError = new CloudResourceStatus(null, resourceStatus, STATUS_REASON_ERROR, PRIVATE_ID_1);
        CloudContext cloudContext = mock(CloudContext.class);
        when(resourcesStatePollerResult.getResults()).thenReturn(List.of(cloudResourceStatusError));

        CloudConnectorException actualException = assertThrows(CloudConnectorException.class,
                () -> underTest.validateResourcesState(cloudContext, resourcesStatePollerResult));

        assertThat(actualException).hasMessageStartingWith("Failed to deploy the database stack for ");
        assertThat(actualException).hasMessageEndingWith(String.format(" due to: %s", STATUS_REASON_ERROR));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("doAcceptTestWhenFailureBadPollerResultDataProvider")
    void testValidateResourcesStateMultipleResources(String testCaseName, ResourceStatus resourceStatus) {
        ResourcesStatePollerResult resourcesStatePollerResult = mock(ResourcesStatePollerResult.class);
        CloudResourceStatus cloudResourceStatusSuccess = new CloudResourceStatus(null, ResourceStatus.CREATED, STATUS_REASON_SUCCESS, PRIVATE_ID_2);
        CloudResourceStatus cloudResourceStatusError = new CloudResourceStatus(null, resourceStatus, STATUS_REASON_ERROR, PRIVATE_ID_1);
        CloudContext cloudContext = mock(CloudContext.class);
        when(resourcesStatePollerResult.getResults()).thenReturn(List.of(cloudResourceStatusSuccess, cloudResourceStatusError));

        CloudConnectorException actualException = assertThrows(CloudConnectorException.class,
                () -> underTest.validateResourcesState(cloudContext, resourcesStatePollerResult));

        assertThat(actualException).hasMessageStartingWith("Failed to deploy the database stack for ");
        assertThat(actualException).hasMessageEndingWith(String.format(" due to: [%s]", cloudResourceStatusError.toString()));
    }

    @Test
    void testValidateResourcesStateNoError() {
        ResourcesStatePollerResult resourcesStatePollerResult = mock(ResourcesStatePollerResult.class);
        CloudResourceStatus cloudResourceStatusSuccess = new CloudResourceStatus(null, ResourceStatus.CREATED, STATUS_REASON_SUCCESS, PRIVATE_ID_2);
        CloudResourceStatus cloudResourceStatusError = new CloudResourceStatus(null, ResourceStatus.CREATED, STATUS_REASON_SUCCESS, PRIVATE_ID_1);
        CloudContext cloudContext = mock(CloudContext.class);
        when(resourcesStatePollerResult.getResults()).thenReturn(List.of(cloudResourceStatusSuccess, cloudResourceStatusError));

        underTest.validateResourcesState(cloudContext, resourcesStatePollerResult);
    }

    static Object[][] doAcceptTestWhenFailureBadPollerResultDataProvider() {
        return new Object[][]{
                // testCaseName resourceStatus
                {"ResourceStatus.DELETED", ResourceStatus.DELETED},
                {"ResourceStatus.FAILED", ResourceStatus.FAILED},
        };
    }
}
