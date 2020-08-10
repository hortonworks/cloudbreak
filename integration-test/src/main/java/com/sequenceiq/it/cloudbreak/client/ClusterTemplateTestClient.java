package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.clustertemplate.ClusterTemplateCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.clustertemplate.ClusterTemplateDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.clustertemplate.ClusterTemplateGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.clustertemplate.ClusterTemplateListAction;
import com.sequenceiq.it.cloudbreak.action.v4.clustertemplate.DeleteClusterFromClusterTemplateAction;
import com.sequenceiq.it.cloudbreak.action.v4.clustertemplate.LaunchClusterFromClusterTemplateAction;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTemplateTestDto;

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

    public Action<ClusterTemplateTestDto, CloudbreakClient> deleteCluster(String stackTemplateKey) {
        return new DeleteClusterFromClusterTemplateAction(stackTemplateKey);
    }

    public Action<ClusterTemplateTestDto, CloudbreakClient> deleteCluster(Class<StackTemplateTestDto> stackTemplateKey) {
        return new DeleteClusterFromClusterTemplateAction(stackTemplateKey);
    }

    public Action<ClusterTemplateTestDto, CloudbreakClient> launchCluster(String stackTemplateKey) {
        return new LaunchClusterFromClusterTemplateAction(stackTemplateKey);
    }

    public Action<ClusterTemplateTestDto, CloudbreakClient> launchCluster(Class<StackTemplateTestDto> stackTemplateKey) {
        return new LaunchClusterFromClusterTemplateAction(stackTemplateKey);
    }

}