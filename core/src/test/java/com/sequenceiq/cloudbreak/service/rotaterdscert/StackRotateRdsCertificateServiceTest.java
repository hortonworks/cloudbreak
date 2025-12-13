package com.sequenceiq.cloudbreak.service.rotaterdscert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateRdsCertResponseType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class StackRotateRdsCertificateServiceTest {

    private static final String CRN = "stackCrn";

    private static final String ACCOUNT_ID = "1";

    private static final FlowIdentifier FLOW_IDENTIFIER = new FlowIdentifier(FlowType.FLOW, "flowId");

    @Mock
    private StackDtoService stackService;

    @Mock
    private StackCommonService stackCommonService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private BlueprintService blueprintService;

    @InjectMocks
    private StackRotateRdsCertificateService underTest;

    @Test
    void testRotateRdsCertificateWhenEntitledForValidationSkipping() {
        Stack stack = TestUtil.stack();
        when(entitlementService.cdpSkipRdsSslCertificateRollingRotationValidation(ACCOUNT_ID)).thenReturn(true);
        when(stackService.getStackViewByNameOrCrn(NameOrCrn.ofCrn(CRN), ACCOUNT_ID)).thenReturn(stack);
        when(stackCommonService.rotateRdsCertificate(stack)).thenReturn(FLOW_IDENTIFIER);
        StackRotateRdsCertificateV4Response response = underTest.rotateRdsCertificate(NameOrCrn.ofCrn(CRN), ACCOUNT_ID);
        verify(stackCommonService).rotateRdsCertificate(stack);
        assertThat(response.getResponseType()).isEqualTo(RotateRdsCertResponseType.TRIGGERED);
        assertThat(response.getResourceCrn()).isEqualTo(stack.getResourceCrn());
        assertThat(response.getReason()).isNull();
        assertThat(response.getFlowIdentifier()).isEqualTo(FLOW_IDENTIFIER);
    }

    @Test
    void testRotateRdsCertificateWhenClusterIsCapableOfRollingServiceRestart() {
        Stack stack = TestUtil.stack(TestUtil.cluster());
        Blueprint blueprint = mock(Blueprint.class);
        when(blueprintService.getByClusterId(anyLong())).thenReturn(Optional.of(blueprint));
        when(blueprint.getBlueprintUpgradeOption()).thenReturn(BlueprintUpgradeOption.ROLLING_UPGRADE_ENABLED);
        when(stackService.getStackViewByNameOrCrn(NameOrCrn.ofCrn(CRN), ACCOUNT_ID)).thenReturn(stack);
        when(stackCommonService.rotateRdsCertificate(stack)).thenReturn(FLOW_IDENTIFIER);
        StackRotateRdsCertificateV4Response response = underTest.rotateRdsCertificate(NameOrCrn.ofCrn(CRN), ACCOUNT_ID);
        verify(stackCommonService).rotateRdsCertificate(stack);
        assertThat(response.getResponseType()).isEqualTo(RotateRdsCertResponseType.TRIGGERED);
        assertThat(response.getResourceCrn()).isEqualTo(stack.getResourceCrn());
        assertThat(response.getReason()).isNull();
        assertThat(response.getFlowIdentifier()).isEqualTo(FLOW_IDENTIFIER);
    }

    @Test
    void testRotateRdsCertificateWhenAccountIsNotEntitledAndClusterUpgradeOptionIsNull() {
        Stack stack = TestUtil.stack(TestUtil.cluster());
        when(stackService.getStackViewByNameOrCrn(NameOrCrn.ofCrn(CRN), ACCOUNT_ID)).thenReturn(stack);
        Blueprint blueprint = mock(Blueprint.class);
        when(blueprintService.getByClusterId(anyLong())).thenReturn(Optional.of(blueprint));
        when(blueprint.getBlueprintUpgradeOption()).thenReturn(null);
        when(entitlementService.cdpSkipRdsSslCertificateRollingRotationValidation(ACCOUNT_ID)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.rotateRdsCertificate(NameOrCrn.ofCrn(CRN), ACCOUNT_ID));

        assertEquals("The cluster is not supporting rolling restart of services and you are not entitled to use RDS SSL certificate rotation " +
                "without rolling restart. Please contact Cloudera to enable 'CDP_SKIP_CERTIFICATE_ROTATION_VALIDATION' entitlement for your account",
                exception.getMessage());
        verifyNoInteractions(stackCommonService);
    }

    @Test
    void testRotateRdsCertificateWhenAccountIsNotEntitledAndClusterIsNotCapableOfRollingRestartOfServices() {
        Stack stack = TestUtil.stack(TestUtil.cluster());
        when(stackService.getStackViewByNameOrCrn(NameOrCrn.ofCrn(CRN), ACCOUNT_ID)).thenReturn(stack);
        Blueprint blueprint = mock(Blueprint.class);
        when(blueprintService.getByClusterId(anyLong())).thenReturn(Optional.of(blueprint));
        when(blueprint.getBlueprintUpgradeOption()).thenReturn(BlueprintUpgradeOption.DISABLED);
        when(entitlementService.cdpSkipRdsSslCertificateRollingRotationValidation(ACCOUNT_ID)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.rotateRdsCertificate(NameOrCrn.ofCrn(CRN), ACCOUNT_ID));

        assertEquals("The cluster is not supporting rolling restart of services and you are not entitled to use RDS SSL certificate rotation " +
                "without rolling restart. Please contact Cloudera to enable 'CDP_SKIP_CERTIFICATE_ROTATION_VALIDATION' entitlement for your account",
                exception.getMessage());
        verifyNoInteractions(stackCommonService);
    }
}