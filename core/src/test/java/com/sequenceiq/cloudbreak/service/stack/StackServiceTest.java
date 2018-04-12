package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import static com.sequenceiq.cloudbreak.api.model.InstanceMetadataType.CORE;
import static com.sequenceiq.cloudbreak.api.model.InstanceMetadataType.GATEWAY;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StackServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_ID = "instanceId";

    private static final String INSTANCE_PUBLIC_IP = "2.2.2.2";

    private static final String OWNER = "1234567";

    private static final String USER_ID = OWNER;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
    private IdentityUser user;

    @Test
    public void testRemoveInstanceWhenTheInstanceIsCoreTypeAndUserHasRightToTerminateThenThenProcessWouldBeSuccessful() {
        when(stackRepository.findOne(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(stack.isPublicInAccount()).thenReturn(true);
        when(stack.getOwner()).thenReturn(OWNER);
        when(user.getUserId()).thenReturn(USER_ID);
        when(instanceMetaData.getPublicIp()).thenReturn(INSTANCE_PUBLIC_IP);
        when(instanceMetaData.getInstanceMetadataType()).thenReturn(CORE);
        doNothing().when(downscaleValidatorService).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, CORE);
        doNothing().when(downscaleValidatorService).checkUserHasRightToTerminateInstance(true, OWNER, USER_ID, STACK_ID);
        doNothing().when(flowManager).triggerStackRemoveInstance(anyLong(), anyString(), anyString());

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
        verify(instanceMetaDataRepository, times(1)).findByInstanceId(STACK_ID, INSTANCE_ID);
        verify(downscaleValidatorService, times(1)).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, CORE);
        verify(downscaleValidatorService, times(1)).checkUserHasRightToTerminateInstance(true, OWNER, USER_ID,
                STACK_ID);
    }

    @Test
    public void testRemoveInstanceWhenTheHostIsGatewayTypeThenWeShoulNotAllowTerminationWithException() {
        String exceptionMessage = String.format("Downscale for node [public IP: %s] is prohibited because it maintains the Ambari server", INSTANCE_PUBLIC_IP);
        when(stackRepository.findOne(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(instanceMetaData.getInstanceMetadataType()).thenReturn(GATEWAY);
        when(instanceMetaData.getPublicIp()).thenReturn(INSTANCE_PUBLIC_IP);
        doThrow(new BadRequestException(exceptionMessage)).when(downscaleValidatorService).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY);

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(exceptionMessage);

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
        verify(instanceMetaDataRepository, times(1)).findByInstanceId(STACK_ID, INSTANCE_ID);
        verify(downscaleValidatorService, times(1)).checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY);
        verify(downscaleValidatorService, times(0)).checkUserHasRightToTerminateInstance(anyBoolean(), anyString(), anyString(),
                anyLong());
        verify(flowManager, times(0)).triggerStackRemoveInstance(anyLong(), anyString(), anyString());
    }

    @Test
    public void testWhenTheUserHasNoRightToModifyTheStackThenExceptionWouldThrown() {
        String exceptionMessage = "Private stack (%s) is only modifiable by the owner.";
        String userId = "222";
        String owner = "111";
        when(stackRepository.findOne(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(stack.isPublicInAccount()).thenReturn(false);
        when(stack.getOwner()).thenReturn(owner);
        when(user.getUserId()).thenReturn(userId);
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
        verify(flowManager, times(0)).triggerStackRemoveInstance(anyLong(), anyString(), anyString());
    }

    @Test
    public void testWhenInstanceMetaDataIsNullThenExceptionWouldThrown() {
        when(stackRepository.findOne(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(null);
        doNothing().when(authorizationService).hasReadPermission(stack);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format("Metadata for instance %s has not found.", INSTANCE_ID));

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
    }

    @Test
    public void testWhenStackCouldNotFindByItsIdThenExceptionWouldThrown() {
        when(stackRepository.findOne(STACK_ID)).thenReturn(null);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format("Stack '%s' has not found", STACK_ID));

        underTest.get(STACK_ID);
    }

}
