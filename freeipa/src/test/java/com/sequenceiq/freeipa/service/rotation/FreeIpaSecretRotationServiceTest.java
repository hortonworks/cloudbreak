package com.sequenceiq.freeipa.service.rotation;

import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.SALT_BOOT_SECRETS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaSecretRotationServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String ACCOUNT_ID = "account1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:envCrn1";

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private SecretRotationValidationService secretRotationValidationService;

    @InjectMocks
    private FreeIpaSecretRotationService underTest;

    @Captor
    private ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);

    @BeforeEach
    void setup() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "enabledSecretTypes", List.of(FreeIpaSecretType.values()), true);
    }

    @Test
    public void testSecretRotationIsTriggered() {
        when(secretRotationValidationService.validate(any(), any(), any(), any())).thenReturn(Optional.of(RotationFlowExecutionType.ROTATE));
        Stack stack = newStack(DetailedStackStatus.AVAILABLE);
        when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "chain-id");
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);

        FreeIpaSecretRotationRequest request = new FreeIpaSecretRotationRequest();
        request.setExecutionType(RotationFlowExecutionType.ROTATE);
        request.setSecrets(List.of(SALT_BOOT_SECRETS.name()));

        FlowIdentifier result = underTest.rotateSecretsByCrn(ACCOUNT_ID, ENV_CRN, request);

        assertEquals(flowIdentifier, result);
        verify(flowManager).notify(eq("SECRETROTATIONFLOWCHAINTRIGGEREVENT"), captor.capture());
        verify(secretRotationValidationService, times(1)).validateEnabledSecretTypes(eq(List.of(SALT_BOOT_SECRETS)), isNull());
        Acceptable acceptable = captor.getValue();
        assertInstanceOf(SecretRotationFlowChainTriggerEvent.class, acceptable);
        SecretRotationFlowChainTriggerEvent event = (SecretRotationFlowChainTriggerEvent) acceptable;
        assertEquals(RotationFlowExecutionType.ROTATE, event.getExecutionType());
        assertEquals(List.of(SALT_BOOT_SECRETS), event.getSecretTypes());
    }

    private Stack newStack(DetailedStackStatus status) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setStackStatus(new StackStatus(null, null, status));
        return stack;
    }
}