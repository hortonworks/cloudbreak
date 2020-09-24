package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.clustertemplate.ClusterTemplateCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.clustertemplate.ClusterTemplateDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.clustertemplate.ClusterTemplateGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.clustertemplate.ClusterTemplateListAction;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;

@Service
public class ClusterTemplateTestClient {

    public Action<ClusterTemplateTestDto, CloudbreakClient> createV4() {
        return new ClusterTemplateCreateAction();
    }

    public Action<ClusterTemplateTestDto, CloudbreakClient> getV4() {
        return new ClusterTemplateGetAction();
    }

    public Action<ClusterTemplateTestDto, CloudbreakClient> listV4() {
        return new ClusterTemplateListAction();
    }

    public Action<ClusterTemplateTestDto, CloudbreakClient> deleteV4() {
        return new ClusterTemplateDeleteAction();
    }

}