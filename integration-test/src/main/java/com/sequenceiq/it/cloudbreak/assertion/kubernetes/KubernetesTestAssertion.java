package com.sequenceiq.it.cloudbreak.assertion.kubernetes;

import com.sequenceiq.it.cloudbreak.dto.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;

public class KubernetesTestAssertion {

    private KubernetesTestAssertion() {

    }

    public static Assertion<KubernetesTestDto> listContains(String kubernetesName, Integer expectedCount) {
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
