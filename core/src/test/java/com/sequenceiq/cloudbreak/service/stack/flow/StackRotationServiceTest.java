package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_ADMIN_PASSWORD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
public class StackRotationServiceTest {

    private static final String CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:878605d9-f9e9-44c6-9da6-e4bce9570ef5";

    @InjectMocks
    private StackRotationService underTest;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private SecretRotationValidationService secretRotationValidationService;

    private static StackIdView getStackIdView() {
        return new StackIdView() {

            @Override
            public Long getId() {
                return null;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getCrn() {
                return CRN;
            }
        };
    }

    @BeforeEach
    void setup() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "enabledSecretTypes", List.of(CloudbreakSecretType.values()), true);
    }

    @Test
    public void testRotateSecrets() {
        Stack stack = new Stack();
        stack.setId(1L);
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stack);
        when(secretRotationValidationService.validate(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(flowManager.triggerSecretRotation(anyLong(), anyString(), any(), any(), anyMap())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "flowchain"));

        underTest.rotateSecrets(CRN, List.of(CM_ADMIN_PASSWORD.name()), null, Map.of());

        verify(stackDtoService).getStackViewByCrn(eq(CRN));
        verify(secretRotationValidationService).validate(eq(CRN), eq(List.of(CM_ADMIN_PASSWORD)), eq(null), any());
        verify(secretRotationValidationService).validateEnabledSecretTypes(eq(List.of(CM_ADMIN_PASSWORD)), isNull());
        verify(flowManager).triggerSecretRotation(anyLong(), anyString(), any(), any(), anyMap());
    }
}
