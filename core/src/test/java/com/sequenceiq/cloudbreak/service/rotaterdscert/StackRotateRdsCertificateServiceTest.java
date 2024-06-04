package com.sequenceiq.cloudbreak.service.rotaterdscert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateRdsCertResponseType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class StackRotateRdsCertificateServiceTest {

    private static final String CRN = "stackCrn";

    private static final Long WORKSPACE_ID = 1L;

    private static final FlowIdentifier FLOW_IDENTIFIER = new FlowIdentifier(FlowType.FLOW, "flowId");

    @Mock
    private StackService stackService;

    @Mock
    private StackCommonService stackCommonService;

    @InjectMocks
    private StackRotateRdsCertificateService underTest;

    @Test
    void testRotateRdsCertificate() {
        Stack stack = TestUtil.stack();
        when(stackService.getByNameOrCrnInWorkspace(NameOrCrn.ofCrn(CRN), WORKSPACE_ID)).thenReturn(stack);
        when(stackCommonService.rotateRdsCertificate(stack)).thenReturn(FLOW_IDENTIFIER);
            StackRotateRdsCertificateV4Response response = underTest.rotateRdsCertificate(NameOrCrn.ofCrn(CRN), WORKSPACE_ID);
        verify(stackCommonService).rotateRdsCertificate(stack);
        assertThat(response.getResponseType()).isEqualTo(RotateRdsCertResponseType.TRIGGERED);
        assertThat(response.getResourceCrn()).isEqualTo(stack.getResourceCrn());
        assertThat(response.getReason()).isNull();
        assertThat(response.getFlowIdentifier()).isEqualTo(FLOW_IDENTIFIER);
    }
}
