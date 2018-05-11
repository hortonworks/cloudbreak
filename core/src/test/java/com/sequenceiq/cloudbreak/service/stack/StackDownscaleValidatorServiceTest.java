package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType.CORE;
import static com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType.GATEWAY;
import static com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType.GATEWAY_PRIMARY;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;

@RunWith(MockitoJUnitRunner.class)
public class StackDownscaleValidatorServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_PUBLIC_IP = "2.2.2.2";

    private static final String AMBARI_SERVER_HOST_EXCEPTION_MESSAGE = String.format("Downscale for node [public IP: %s] is prohibited because it serves as "
            + "a host the Ambari server", INSTANCE_PUBLIC_IP);

    private static final String ACCESS_DENIED_EXCEPTION_MESSAGE = String.format("Private stack (%s) is only modifiable by the owner.", STACK_ID);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private StackDownscaleValidatorService underTest;

    @Test
    public void testCheckInstanceIsTheAmbariServerOrNotMethodWhenInstanceIsCoreTypeThenNoExceptionWouldInvoke() {
        underTest.checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, CORE);
    }

    @Test
    public void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayThenException() {
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(AMBARI_SERVER_HOST_EXCEPTION_MESSAGE);

        underTest.checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY);
    }

    @Test
    public void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayPrimaryThenException() {
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(AMBARI_SERVER_HOST_EXCEPTION_MESSAGE);

        underTest.checkInstanceIsTheAmbariServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY_PRIMARY);
    }

    @Test
    public void testCheckUserHasRightToTerminateInstanceMethodWhenPublicInAccountAndOwnerAndUserIdIsSameThenNoException() {
        underTest.checkUserHasRightToTerminateInstance(true, "same", "same", STACK_ID);
    }

    @Test
    public void testCheckUserHasRightToTerminateInstanceMethodWhenPublicInAccountAndUserIdAndOwnerAreNotTheSameThenNoException() {
        underTest.checkUserHasRightToTerminateInstance(true, "something here", "something else here", STACK_ID);
    }

    @Test
    public void testCheckUserHasRightToTerminateInstanceMethodWhenNotPublicInAccountAndUserIdAndOwnerAreNotTheSameThenExceptionWouldInvoke() {
        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(ACCESS_DENIED_EXCEPTION_MESSAGE);

        underTest.checkUserHasRightToTerminateInstance(false, "something here", "something else here", STACK_ID);
    }

}
