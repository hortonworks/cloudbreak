package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretRotationRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXRotateSecretInternalAction implements Action<DistroXTestDto, CloudbreakClient> {

    private final Collection<CloudbreakSecretType> secretTypes;

    private final Map<String, String> additionalProperties;

    public DistroXRotateSecretInternalAction(Collection<CloudbreakSecretType> secretTypes, Map<String, String> additionalProperties) {
        this.secretTypes = secretTypes;
        this.additionalProperties = additionalProperties;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        DistroXSecretRotationRequest request = new DistroXSecretRotationRequest();
        request.setSecrets(secretTypes.stream().map(Enum::name).collect(Collectors.toList()));
        request.setCrn(testDto.getCrn());
        request.setAdditionalProperties(additionalProperties);
        testDto.setFlow("Data Hub secret rotation.", client.getInternalClient(testContext).distroXV1RotationEndpoint().rotateSecrets(request));
        return testDto;
    }
}
