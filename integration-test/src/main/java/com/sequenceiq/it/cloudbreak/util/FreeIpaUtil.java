package com.sequenceiq.it.cloudbreak.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeipaSecretTypeResponse;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

@Component
public class FreeIpaUtil {

    public List<FreeipaSecretTypeResponse> getSecretTypes(AbstractFreeIpaTestDto testDto, FreeIpaClient freeIpaClient) {
        return freeIpaClient.getDefaultClient(testDto.getTestContext()).getFreeipaRotationV1Endpoint().listRotatableFreeipaSecretType(testDto.getCrn());
    }
}
