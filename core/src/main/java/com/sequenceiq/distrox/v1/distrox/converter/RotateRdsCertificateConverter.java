package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.rotaterdscert.RotateRdsCertificateV1Response;

@Component
public class RotateRdsCertificateConverter {

    public RotateRdsCertificateV1Response convert(StackRotateRdsCertificateV4Response source) {
        return new RotateRdsCertificateV1Response(source.getResponseType(), source.getFlowIdentifier(), source.getReason(), source.getResourceCrn());
    }

}
