package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;

public class AwsRdsTerminateServiceTest {

    private static final String STACK_NAME = "stack name";

    private static final String REGION = "region";

    private static final Long ID = 1L;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsClient awsClient;

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
    private DeleteStackRequest deleteStackRequest;

    @Mock
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @Mock
    private AmazonCloudFormationWaiters amazonCloudFormationWaiters;

    @Mock
    private Waiter<DescribeStacksRequest> describeStacksRequestWaiter;

    @Mock
    private PersistenceNotifier persistenceNotifier;

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
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), Mockito.eq(REGION))).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.waiters()).thenReturn(amazonCloudFormationWaiters);
        when(amazonCloudFormationWaiters.stackDeleteComplete()).thenReturn(describeStacksRequestWaiter);
        when(cloudContext.getId()).thenReturn(ID);

    }

    @Test
    public void shouldCallDeleteStackAndWaitForDeleteComplete() throws Exception {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), Mockito.eq(REGION))).thenReturn(amazonCloudFormationClient);

        victim.terminate(authenticatedContext, null, false, persistenceNotifier, Collections.emptyList());

        verify(amazonCloudFormationClient).deleteStack(deleteStackRequest);
        verify(describeStacksRequestWaiter).run(Mockito.any(WaiterParameters.class));
    }

    @Test
    public void shouldNotCallDeleteWhenAlreadyDeleted() throws Exception {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), Mockito.eq(REGION)))
                .thenThrow(new AmazonServiceException(STACK_NAME + " does not exist"));

        victim.terminate(authenticatedContext, null, false, persistenceNotifier, Collections.emptyList());

        verifyNoInteractions(amazonCloudFormationClient);
    }

    @Test
    public void shouldThrowAmazonServiceExceptionWhenDescriptionNotDoesNotExists() {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), Mockito.eq(REGION))).thenThrow(new AmazonServiceException(""));

        Assertions.assertThrows(AmazonServiceException.class,
                () -> victim.terminate(authenticatedContext, null, false, persistenceNotifier, Collections.emptyList()));
    }

    @Test
    public void shouldNotThrowAmazonServiceExceptionWhenItWasThrownAndTerminationIsForced() throws Exception {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), Mockito.eq(REGION))).thenThrow(new AmazonServiceException(""));

        victim.terminate(authenticatedContext, null, true, persistenceNotifier, Collections.emptyList());

        verifyNoInteractions(amazonCloudFormationClient);
    }

    @Test
    public void shouldWrapRuntineExceptionWithCloudConnectorException() {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), Mockito.eq(REGION))).thenThrow(new RuntimeException());

        Assertions.assertThrows(CloudConnectorException.class,
                () -> victim.terminate(authenticatedContext, null, false, persistenceNotifier, Collections.emptyList()));
    }

    @Test
    public void shouldNotFailOnRuntimeExceptionWhenTerminationIsForced() throws Exception {
        when(awsClient.createCloudFormationClient(Mockito.any(AwsCredentialView.class), Mockito.eq(REGION))).thenThrow(new RuntimeException());

        victim.terminate(authenticatedContext, null, true, persistenceNotifier, Collections.emptyList());

        verifyNoInteractions(amazonCloudFormationClient);
    }
}