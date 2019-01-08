package com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses;

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel
public class KubernetesV4Responses {

    private Set<KubernetesV4Response> kuberneteses = new HashSet<>();

    public Set<KubernetesV4Response> getKuberneteses() {
        return kuberneteses;
    }

    public void setKuberneteses(Set<KubernetesV4Response> kuberneteses) {
        this.kuberneteses = kuberneteses;
    }

    public static final KubernetesV4Responses kubernetesV4Responses(Set<KubernetesV4Response> kubernetesV4Responses) {
        KubernetesV4Responses kubernetesV4Responses1 = new KubernetesV4Responses();
        kubernetesV4Responses1.setKuberneteses(kubernetesV4Responses);
        return kubernetesV4Responses1;
    }
}
