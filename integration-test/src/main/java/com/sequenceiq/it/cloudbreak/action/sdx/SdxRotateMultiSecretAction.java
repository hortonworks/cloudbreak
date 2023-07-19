package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxMultiSecretRotationRequest;
import com.sequenceiq.sdx.rotation.DatalakeMultiSecretType;

public class SdxRotateMultiSecretAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRotateMultiSecretAction.class);

    private final DatalakeMultiSecretType multiSecretType;

    public SdxRotateMultiSecretAction(DatalakeMultiSecretType multiSecretType) {
        this.multiSecretType = multiSecretType;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        SdxMultiSecretRotationRequest request = new SdxMultiSecretRotationRequest();
        request.setCrn(testDto.getCrn());
        request.setSecret(multiSecretType.value());
        FlowIdentifier flowIdentifier = client.getDefaultClient().sdxRotationEndpoint().rotateMultiSecrets(request);
        testDto.setFlow("secret rotation", flowIdentifier);
        return testDto;
    }
}
