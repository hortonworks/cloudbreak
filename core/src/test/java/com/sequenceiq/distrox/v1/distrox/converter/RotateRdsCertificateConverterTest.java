package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateRdsCertResponseType;
import com.sequenceiq.distrox.api.v1.distrox.model.rotaterdscert.RotateRdsCertificateV1Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

class RotateRdsCertificateConverterTest {

    private static final String CRN = "crn";

    private static final String REASON = "reason";

    private static final FlowIdentifier FLOW_IDENTIFIER = new FlowIdentifier(FlowType.FLOW, "stackId");

    private final RotateRdsCertificateConverter underTest = new RotateRdsCertificateConverter();

    @Test
    void testConvert() {
        StackRotateRdsCertificateV4Response source = new StackRotateRdsCertificateV4Response(RotateRdsCertResponseType.TRIGGERED, FLOW_IDENTIFIER, REASON, CRN);
        RotateRdsCertificateV1Response result = underTest.convert(source);

        assertThat(result.getFlowIdentifier()).isEqualTo(FLOW_IDENTIFIER);
        assertThat(result.getReason()).isEqualTo(REASON);
        assertThat(result.getResourceCrn()).isEqualTo(CRN);
        assertThat(result.getResponseType()).isEqualTo(RotateRdsCertResponseType.TRIGGERED);
    }
}
