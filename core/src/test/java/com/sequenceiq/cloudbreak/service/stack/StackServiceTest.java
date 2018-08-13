package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType.CORE;
import static com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType.GATEWAY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.user.UserService;

@RunWith(MockitoJUnitRunner.class)
public class StackServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_ID = "instanceId";

    private static final String INSTANCE_ID2 = "instanceId2";

    private static final String INSTANCE_PUBLIC_IP = "2.2.2.2";

    private static final String INSTANCE_PUBLIC_IP2 = "3.3.3.3";

    private static final String OWNER = "1234567";

    private static final String USER_ID = OWNER;

    private static final String VARIANT_VALUE = "VARIANT_VALUE";

    private static final String IMAGE_CATALOG = "IMAGE_CATALOG";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private StackService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private StackDownscaleValidatorService downscaleValidatorService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private Stack stack;

    @Mock
    private InstanceMetaData instanceMetaData;

    @Mock
    private InstanceMetaData instanceMetaData2;

    @Mock
    private IdentityUser user;

    @Mock
    private ServiceProviderConnectorAdapter connector;

    @Mock
    private Variant variant;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private ComponentConfigProvider componentConfigProvider;

    @Mock
    private InstanceGroupRepository instanceGroupRepository;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private SecurityConfig securityConfig;

    @Mock
    private SecurityConfigRepository securityConfigRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private PlatformParameters parameters;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private TransactionService transactionService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private UserService userService;

    @Before
    public void setup() throws TransactionExecutionException {
        doNothing().when(flowManager).triggerStackStop(anyObject());
        doNothing().when(flowManager).triggerStackStart(anyObject());
    }

    @Test
    public void testRemoveInstanceWhenTheInstanceIsCoreTypeAndUserHasRightToTerminateThenThenProcessWouldBeSuccessful() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(stack.isPublicInAccount()).thenReturn(true);
        when(stack.getOwner()).thenReturn(OWNER);
        when(stack.getId()).thenReturn(STACK_ID);
        when(user.getUserId()).thenReturn(USER_ID);
        when(instanceMetaData.getPublicIp()).thenReturn(INSTANCE_PUBLIC_IP);
        when(instanceMetaData.getInstanceMetadataType()).thenReturn(CORE);
        doNothing().when(downscaleValidatorService).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, CORE);
        doNothing().when(downscaleValidatorService).checkUserHasRightToTerminateInstance(true, OWNER, USER_ID, STACK_ID);

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
        verify(instanceMetaDataRepository, times(1)).findByInstanceId(STACK_ID, INSTANCE_ID);
        verify(downscaleValidatorService, times(1)).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, CORE);
        verify(downscaleValidatorService, times(1)).checkUserHasRightToTerminateInstance(true, OWNER, USER_ID,
                STACK_ID);
    }

    @Test
    public void testRemoveInstancesWhenTheInstancesAreCoreTypeAndUserHasRightToTerminateThenProcessWouldBeSuccessful() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID2)).thenReturn(instanceMetaData2);
        when(stack.isPublicInAccount()).thenReturn(true);
        when(stack.getOwner()).thenReturn(OWNER);
        when(user.getUserId()).thenReturn(USER_ID);
        when(stack.getId()).thenReturn(STACK_ID);
        when(instanceMetaData.getPublicIp()).thenReturn(INSTANCE_PUBLIC_IP);
        when(instanceMetaData.getInstanceMetadataType()).thenReturn(CORE);
        when(instanceMetaData2.getPublicIp()).thenReturn(INSTANCE_PUBLIC_IP2);
        when(instanceMetaData2.getInstanceMetadataType()).thenReturn(CORE);
        doNothing().when(downscaleValidatorService).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, CORE);
        doNothing().when(downscaleValidatorService).checkUserHasRightToTerminateInstance(true, OWNER, USER_ID, STACK_ID);

        underTest.removeInstances(user, STACK_ID, Sets.newHashSet(INSTANCE_ID, INSTANCE_ID2));
        verify(instanceMetaDataRepository, times(2)).findByInstanceId(eq(STACK_ID), anyString());
        verify(downscaleValidatorService, times(1)).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, CORE);
        verify(downscaleValidatorService, times(1)).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP2, CORE);
        verify(downscaleValidatorService, times(2)).checkUserHasRightToTerminateInstance(true, OWNER, USER_ID,
                STACK_ID);
        verify(flowManager, times(1)).triggerStackRemoveInstances(eq(STACK_ID), anyMap());
    }

    @Test
    public void testRemoveInstanceWhenTheHostIsGatewayTypeThenWeShoulNotAllowTerminationWithException() {
        String exceptionMessage = String.format("Downscale for node [public IP: %s] is prohibited because it maintains the Ambari server", INSTANCE_PUBLIC_IP);
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(instanceMetaData.getInstanceMetadataType()).thenReturn(GATEWAY);
        when(stack.getId()).thenReturn(STACK_ID);
        when(instanceMetaData.getPublicIp()).thenReturn(INSTANCE_PUBLIC_IP);
        doThrow(new BadRequestException(exceptionMessage)).when(downscaleValidatorService).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY);

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(exceptionMessage);

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
        verify(instanceMetaDataRepository, times(1)).findByInstanceId(STACK_ID, INSTANCE_ID);
        verify(downscaleValidatorService, times(1)).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY);
        verify(downscaleValidatorService, times(0)).checkUserHasRightToTerminateInstance(anyBoolean(), anyString(), anyString(),
                anyLong());
        verify(flowManager, times(0)).triggerStackRemoveInstance(anyLong(), anyString(), anyLong());
    }

    @Test
    public void testRemoveInstancesWhenOneHostIsGatewayTypeThenWeShoulNotAllowTerminationWithException() {
        String exceptionMessage = String.format("Downscale for node [public IP: %s] is prohibited because it maintains the Ambari server", INSTANCE_PUBLIC_IP);
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stack.getId()).thenReturn(STACK_ID);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(instanceMetaData.getInstanceMetadataType()).thenReturn(GATEWAY);
        when(instanceMetaData.getPublicIp()).thenReturn(INSTANCE_PUBLIC_IP);
        doThrow(new BadRequestException(exceptionMessage)).when(downscaleValidatorService).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY);

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(exceptionMessage);

        underTest.removeInstances(user, STACK_ID, Sets.newHashSet(INSTANCE_ID, INSTANCE_ID2));
        verify(instanceMetaDataRepository, times(1)).findByInstanceId(STACK_ID, INSTANCE_ID);
        verify(downscaleValidatorService, times(1)).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY);
        verify(downscaleValidatorService, times(0)).checkUserHasRightToTerminateInstance(anyBoolean(), anyString(), anyString(),
                anyLong());
        verify(flowManager, times(0)).triggerStackRemoveInstance(anyLong(), anyString(), anyLong());
    }

    @Test
    public void testWhenTheUserHasNoRightToModifyTheStackThenExceptionWouldThrown() {
        String exceptionMessage = "Private stack (%s) is only modifiable by the owner.";
        String userId = "222";
        String owner = "111";
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(stack.isPublicInAccount()).thenReturn(false);
        when(stack.getOwner()).thenReturn(owner);
        when(user.getUserId()).thenReturn(userId);
        when(stack.getId()).thenReturn(STACK_ID);
        when(instanceMetaData.getPublicIp()).thenReturn(INSTANCE_PUBLIC_IP);
        when(instanceMetaData.getInstanceMetadataType()).thenReturn(CORE);
        doNothing().when(downscaleValidatorService).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, CORE);
        doThrow(new AccessDeniedException(exceptionMessage)).when(downscaleValidatorService).checkUserHasRightToTerminateInstance(false,
                owner, userId, STACK_ID);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(exceptionMessage);

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
        verify(instanceMetaDataRepository, times(1)).findByInstanceId(STACK_ID, INSTANCE_ID);
        verify(downscaleValidatorService, times(1)).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, CORE);
        verify(downscaleValidatorService, times(1)).checkUserHasRightToTerminateInstance(false, owner, userId,
                STACK_ID);
        verify(flowManager, times(0)).triggerStackRemoveInstance(anyLong(), anyString(), anyLong());
    }

    @Test
    public void testWhenInstanceMetaDataIsNullThenExceptionWouldThrown() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stack.getId()).thenReturn(STACK_ID);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(null);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format("Metadata for instance %s has not found.", INSTANCE_ID));

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
    }

    @Test
    public void testWhenInstanceWhenValidateClusterStatus() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        cluster.setStatus(Status.STOPPED);
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(mock(InstanceMetaData.class));
//        doNothing().when(authorizationService).hasReadPermission(stack);

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);

        verify(downscaleValidatorService, times(1)).checkClusterInValidStatus(cluster);
    }

    @Test
    public void testWhenStackCouldNotFindByItsIdThenExceptionWouldThrown() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.ofNullable(null));
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format("Stack '%d' not found", STACK_ID));
        underTest.getById(STACK_ID);
    }

    @Test
    public void testCreateFailsWithInvalidImageId() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);

        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stackAuthentication.passwordAuthenticationRequired()).thenReturn(false);

        when(stackRepository.save(stack)).thenReturn(stack);

        when(tlsSecurityService.storeSSHKeys()).thenReturn(securityConfig);
        when(connector.getPlatformParameters(stack)).thenReturn(parameters);

        expectedException.expectCause(org.hamcrest.Matchers.any(CloudbreakImageNotFoundException.class));

        String platformString = "AWS";
        doThrow(new CloudbreakImageNotFoundException("Image not found"))
                .when(imageService)
                .create(eq(stack), eq(platformString), eq(parameters), nullable(StatedImage.class));

        try {
            stack = underTest.create(user, stack, platformString, mock(StatedImage.class));
        } finally {
            verify(stack, times(1)).setPlatformVariant(eq(VARIANT_VALUE));
            verify(securityConfig, times(1)).setSaltPassword(anyObject());
            verify(securityConfig, times(1)).setSaltBootPassword(anyObject());
            verify(securityConfig, times(1)).setKnoxMasterSecret(anyObject());
            verify(securityConfig, times(1)).setStack(stack);
            verify(securityConfigRepository, times(1)).save(securityConfig);
        }
    }

    @Test
    public void testCreateImageFoundNoStackStatusUpdate() {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);

        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stackAuthentication.passwordAuthenticationRequired()).thenReturn(false);

        when(stackRepository.save(stack)).thenReturn(stack);

        when(tlsSecurityService.storeSSHKeys()).thenReturn(securityConfig);
        when(connector.getPlatformParameters(stack)).thenReturn(parameters);

        try {
            stack = underTest.create(user, stack, "AWS", mock(StatedImage.class));
        } finally {
            verify(stack, times(1)).setPlatformVariant(eq(VARIANT_VALUE));
            verify(securityConfig, times(1)).setSaltPassword(anyObject());
            verify(securityConfig, times(1)).setSaltBootPassword(anyObject());
            verify(securityConfig, times(1)).setKnoxMasterSecret(anyObject());
            verify(securityConfig, times(1)).setStack(stack);
            verify(securityConfigRepository, times(1)).save(securityConfig);

            verify(stackUpdater, times(0)).updateStackStatus(eq(Long.MAX_VALUE), eq(DetailedStackStatus.PROVISION_FAILED), anyString());
        }
    }

    @Test
    public void updateStatusTestStopWhenClusterStoppedThenStackStopTriggered() {
        Stack stack = stack(AVAILABLE, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterService.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterInStopInProgressThenTriggeredStackStopRequested() {
        Stack stack = stack(AVAILABLE, STOP_IN_PROGRESS);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterService.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
        verify(eventService, times(1)).fireCloudbreakEvent(eq(1L), eq(STOP_REQUESTED.name()), nullable(String.class));
    }

    @Test
    public void updateStatusTestStopWhenClusterInStoppedAndStackAvailableThenTriggerStackStop() {
        Stack stack = stack(AVAILABLE, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterService.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterInStoppedAndStackStopFailedThenTriggerStackStop() {
        Stack stack = stack(STOP_FAILED, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterService.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterInStoppedAndStackUpdateInProgressThenBadRequestExceptionDropping() {
        Stack stack = stack(UPDATE_IN_PROGRESS, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterService.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Cannot update the status of stack 'simplestack' to STOPPED, because it isn't in AVAILABLE state.");
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterAndStackAvailableThenBadRequestExceptionDropping() {
        Stack stack = stack(AVAILABLE, AVAILABLE);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterService.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Cannot update the status of stack 'simplestack' to STOPPED, because the cluster is not in STOPPED state.");
        underTest.updateStatus(1L, StatusRequest.STOPPED, false);
    }

    @Test
    public void updateStatusTestStartWhenStackStoppedThenStackStartTriggered() {
        Stack stack = stack(STOPPED, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterService.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STARTED, true);
        verify(flowManager, times(1)).triggerStackStart(anyObject());
    }

    @Test
    public void updateStatusTestStartWhenClusterInStoppedAndStackStopFailedThenBadRequestExceptionDropping() {
        Stack stack = stack(UPDATE_IN_PROGRESS, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterService.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Cannot update the status of stack 'simplestack' to STARTED, because it isn't in STOPPED state.");
        underTest.updateStatus(1L, StatusRequest.STARTED, true);
    }

    @Test
    public void updateStatusTestStartWhenClusterInStoppedAndStackStartFailedThenTriggerStackStart() {
        Stack stack = stack(START_FAILED, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterService.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STARTED, true);
        verify(flowManager, times(1)).triggerStackStart(anyObject());
    }

    @Test
    public void updateStatusTestStartWhenClusterAndStackUpdateInProgressThenBadRequestExceptionDropping() {
        Stack stack = stack(UPDATE_IN_PROGRESS, UPDATE_IN_PROGRESS);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterService.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Cannot update the status of stack 'simplestack' to STARTED, because it isn't in STOPPED state.");
        underTest.updateStatus(1L, StatusRequest.STARTED, true);
    }

    @Test
    public void updateStatusTestStopWhenClusterAndStackAvailableAndEphemeralThenBadRequestExceptionDropping() {
        Stack stack = TestUtil.setEphemeral(TestUtil.stack(AVAILABLE, TestUtil.awsCredential()));
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Cannot stop a stack 'simplestack'. Reason: Instances with ephemeral volumes cannot be stopped.");
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
    }

    @Test
    public void updateStatusTestStopWhenClusterAndStackAvailableAndSpotInstancesThenBadRequestExceptionDropping() {
        Stack stack = TestUtil.setSpotInstances(TestUtil.stack(AVAILABLE, TestUtil.awsCredential()));
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Cannot stop a stack 'simplestack'. Reason: Spot instances cannot be stopped.");
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
    }

    private Stack stack(Status stackStatus, Status clusterStatus) {
        Credential gcpCredential = new Credential();
        Stack stack = new Stack();
        stack.setName("simplestack");
        stack.setStackStatus(new StackStatus(stack, stackStatus, "", DetailedStackStatus.UNKNOWN));
        stack.setCredential(gcpCredential);
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setStatus(clusterStatus);
        cluster.setId(1L);
        stack.setCluster(cluster);
        return stack;
    }
}
