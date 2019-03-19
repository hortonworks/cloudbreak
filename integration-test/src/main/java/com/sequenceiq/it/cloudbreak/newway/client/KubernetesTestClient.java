package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.kubernetes.KubernetesCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.kubernetes.KubernetesCreateIfNotExistAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.kubernetes.KubernetesDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.kubernetes.KubernetesListAction;
import com.sequenceiq.it.cloudbreak.newway.dto.kubernetes.KubernetesTestDto;

@Service
public class KubernetesTestClient {

    public Action<KubernetesTestDto> createV4() {
        return new KubernetesCreateAction();
    }

    public Action<KubernetesTestDto> deleteV4() {
        return new KubernetesDeleteAction();
    }

    public Action<KubernetesTestDto> createIfNotExistV4() {
        return new KubernetesCreateIfNotExistAction();
    }

    public Action<KubernetesTestDto> listV4() {
        return new KubernetesListAction();
    }

}