package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxSecretRotationRequest;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

public class SdxRotateSecretInternalAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRotateSecretInternalAction.class);

    private final Collection<DatalakeSecretType> secretTypes;

    public SdxRotateSecretInternalAction(Collection<DatalakeSecretType> secretTypes) {
        this.secretTypes = secretTypes;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        SdxSecretRotationRequest request = new SdxSecretRotationRequest();
        request.setSecrets(secretTypes.stream().map(Enum::name).collect(Collectors.toList()));
        request.setCrn(testDto.getCrn());
        testDto.setFlow("SDX secret rotation.", client.getInternalClient(testContext).sdxRotationEndpoint().rotateSecrets(request));
        return testDto;
    }
}
