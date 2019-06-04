package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.kubernetes.KubernetesCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.kubernetes.KubernetesCreateIfNotExistAction;
import com.sequenceiq.it.cloudbreak.action.v4.kubernetes.KubernetesDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.kubernetes.KubernetesListAction;
import com.sequenceiq.it.cloudbreak.dto.kubernetes.KubernetesTestDto;

@Service
public class KubernetesTestClient {

    public Action<KubernetesTestDto, CloudbreakClient> createV4() {
        return new KubernetesCreateAction();
    }

    public Action<KubernetesTestDto, CloudbreakClient> deleteV4() {
        return new KubernetesDeleteAction();
    }

    public Action<KubernetesTestDto, CloudbreakClient> createIfNotExistV4() {
        return new KubernetesCreateIfNotExistAction();
    }

    public Action<KubernetesTestDto, CloudbreakClient> listV4() {
        return new KubernetesListAction();
    }

}