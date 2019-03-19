package com.sequenceiq.it.cloudbreak.newway.assertion.kubernetes;

import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.kubernetes.KubernetesTestDto;

public class KubernetesTestAssertion {

    private KubernetesTestAssertion() {

    }

    public static AssertionV2<KubernetesTestDto> listContains(String kubernetesName, Integer expectedCount) {
        return (testContext, entity, cloudbreakClient) -> {
            boolean countCorrect = entity.getResponses()
                    .stream()
                    .filter(credentialV4Response -> credentialV4Response.getName().contentEquals(kubernetesName))
                    .count() == expectedCount;
            if (!countCorrect) {
                throw new IllegalArgumentException("Kubernetes count for " + kubernetesName + " is not as expected!");
            }
            return entity;
        };
    }
}
