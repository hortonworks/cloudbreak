package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretRotationRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXRotateSecretAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRotateSecretAction.class);

    private final Set<CloudbreakSecretType> secretTypes;

    private final RotationFlowExecutionType rotationFlowExecutionType;

    private Map<String, String> additionalParameters;

    public DistroXRotateSecretAction(Set<CloudbreakSecretType> secretTypes, RotationFlowExecutionType rotationFlowExecutionType,
            Map<String, String> additionalParameters) {
        this.secretTypes = secretTypes;
        this.rotationFlowExecutionType = rotationFlowExecutionType;
        this.additionalParameters = additionalParameters;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        DistroXSecretRotationRequest request = new DistroXSecretRotationRequest();
        request.setSecrets(secretTypes.stream().map(Enum::name).collect(Collectors.toList()));
        request.setExecutionType(rotationFlowExecutionType);
        request.setCrn(testDto.getCrn());
        testDto.setFlow("secret rotation", client.getDefaultClient(testContext).distroXV1RotationEndpoint().rotateSecrets(request));
        return testDto;
    }
}
