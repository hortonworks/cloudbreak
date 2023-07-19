package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMultiSecretRotationRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXRotateMultiSecretAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRotateMultiSecretAction.class);

    private final MultiSecretType secretType;

    public DistroXRotateMultiSecretAction(MultiSecretType secretType) {
        this.secretType = secretType;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        DistroXMultiSecretRotationRequest request = new DistroXMultiSecretRotationRequest();
        request.setSecret(secretType.value());
        request.setCrn(testDto.getCrn());
        testDto.setFlow("secret rotation", client.getDefaultClient().distroXV1RotationEndpoint().rotateMultiSecrets(request));
        return testDto;
    }
}
