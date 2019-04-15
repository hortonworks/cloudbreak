package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate.ClusterTemplateCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate.ClusterTemplateDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate.ClusterTemplateGetAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate.ClusterTemplateListAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate.DeleteClusterFromClusterTemplateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clustertemplate.LaunchClusterFromClusterTemplateAction;
import com.sequenceiq.it.cloudbreak.newway.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTemplateTestDto;

@Service
public class ClusterTemplateTestClient {

    public Action<ClusterTemplateTestDto> createV4() {
        return new ClusterTemplateCreateAction();
    }

    public Action<ClusterTemplateTestDto> getV4() {
        return new ClusterTemplateGetAction();
    }

    public Action<ClusterTemplateTestDto> listV4() {
        return new ClusterTemplateListAction();
    }

    public Action<ClusterTemplateTestDto> deleteV4() {
        return new ClusterTemplateDeleteAction();
    }

    public Action<ClusterTemplateTestDto> deleteCluster(String stackTemplateKey) {
        return new DeleteClusterFromClusterTemplateAction(stackTemplateKey);
    }

    public Action<ClusterTemplateTestDto> deleteCluster(Class<StackTemplateTestDto> stackTemplateKey) {
        return new DeleteClusterFromClusterTemplateAction(stackTemplateKey);
    }

    public Action<ClusterTemplateTestDto> launchCluster(String stackTemplateKey) {
        return new LaunchClusterFromClusterTemplateAction(stackTemplateKey);
    }

    public Action<ClusterTemplateTestDto> launchCluster(Class<StackTemplateTestDto> stackTemplateKey) {
        return new LaunchClusterFromClusterTemplateAction(stackTemplateKey);
    }

}