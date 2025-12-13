package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.service.Retry;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;

public class AwsRdsTerminateServiceTest {

    private static final String STACK_NAME = "stack name";

    private static final String REGION = "region";

    private static final Long ID = 1L;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AwsStackRequestHelper awsStackRequestHelper;

    @InjectMocks
    private AwsRdsTerminateService victim;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @Mock
    private CloudFormationWaiter amazonCloudFormationWaiters;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private Retry retryService;

    @Mock
    private AwsRdsParameterGroupService awsRdsParameterGroupService;

    @Mock
    private DatabaseStack databaseStack;

    private final DeleteStackRequest deleteStackRequest = DeleteStackRequest.builder().build();

    @BeforeEach
    public void initTests() {
        initMocks(this);

        when(cfStackUtil.getCfStackName(authenticatedContext)).thenReturn(STACK_NAME);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.value()).thenReturn(REGION);
        when(awsStackRequestHelper.createDeleteStackRequest(STACK_NAME)).thenReturn(deleteStackRequest);
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), eq(REGION))).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.waiters()).thenReturn(amazonCloudFormationWaiters);
        when(cloudContext.getId()).thenReturn(ID);
        when(databaseStack.getDatabaseServer()).thenReturn(mock(DatabaseServer.class));
    }

    @Test
    public void shouldCallDeleteStackAndWaitForDeleteComplete() throws Exception {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), eq(REGION))).thenReturn(amazonCloudFormationClient);
        when(retryService.testWith2SecDelayMax15Times(any())).thenReturn(true);

        victim.terminate(authenticatedContext, databaseStack, false, persistenceNotifier, Collections.emptyList());

        verify(amazonCloudFormationClient).deleteStack(deleteStackRequest);
        verify(amazonCloudFormationWaiters).waitUntilStackDeleteComplete(any(DescribeStacksRequest.class), any(WaiterOverrideConfiguration.class));
    }

    @Test
    public void shouldnotCallDeleteStackWhenCfStackDoesNotExist() throws Exception {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), eq(REGION))).thenReturn(amazonCloudFormationClient);
        when(retryService.testWith2SecDelayMax15Times(any())).thenReturn(false);

        victim.terminate(authenticatedContext, databaseStack, false, persistenceNotifier, Collections.emptyList());

        verify(amazonCloudFormationClient, never()).deleteStack(deleteStackRequest);
        verifyNoInteractions(amazonCloudFormationWaiters);
    }

    @Test
    public void shouldNotCallDeleteWhenAlreadyDeleted() throws Exception {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), eq(REGION)))
                .thenThrow(AwsServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorMessage(STACK_NAME + " does not exist").build()).build());

        victim.terminate(authenticatedContext, databaseStack, false, persistenceNotifier, Collections.emptyList());

        verifyNoInteractions(amazonCloudFormationClient);
    }

    @Test
    public void shouldThrowAmazonServiceExceptionWhenDescriptionNotDoesNotExists() {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), eq(REGION)))
                .thenThrow(AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder().errorMessage("").build()).build());

        assertThrows(AwsServiceException.class,
                () -> victim.terminate(authenticatedContext, databaseStack, false, persistenceNotifier, Collections.emptyList()));
    }

    @Test
    public void shouldNotThrowAmazonServiceExceptionWhenItWasThrownAndTerminationIsForced() throws Exception {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), eq(REGION)))
                .thenThrow(AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder().errorMessage("").build()).build());

        victim.terminate(authenticatedContext, databaseStack, true, persistenceNotifier, Collections.emptyList());

        verifyNoInteractions(amazonCloudFormationClient);
    }

    @Test
    public void shouldWrapRuntineExceptionWithCloudConnectorException() {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), eq(REGION))).thenThrow(new RuntimeException());

        assertThrows(CloudConnectorException.class,
                () -> victim.terminate(authenticatedContext, databaseStack, false, persistenceNotifier, Collections.emptyList()));
    }

    @Test
    public void shouldNotFailOnRuntimeExceptionWhenTerminationIsForced() throws Exception {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), eq(REGION))).thenThrow(new RuntimeException());

        victim.terminate(authenticatedContext, databaseStack, true, persistenceNotifier, Collections.emptyList());

        verifyNoInteractions(amazonCloudFormationClient);
    }
}
