package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clusterdefinition.ClusterDefinitionCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clusterdefinition.ClusterDefinitionDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clusterdefinition.ClusterDefinitionGetAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clusterdefinition.ClusterDefinitionListAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.clusterdefinition.ClusterDefinitionRequestAction;
import com.sequenceiq.it.cloudbreak.newway.dto.clusterdefinition.ClusterDefinitionTestDto;

@Service
public class ClusterDefinitionTestClient {

    public Action<ClusterDefinitionTestDto> createV4() {
        return new ClusterDefinitionCreateAction();
    }

    public Action<ClusterDefinitionTestDto> getV4() {
        return new ClusterDefinitionGetAction();
    }

    public Action<ClusterDefinitionTestDto> listV4() {
        return new ClusterDefinitionListAction();
    }

    public Action<ClusterDefinitionTestDto> deleteV4() {
        return new ClusterDefinitionDeleteAction();
    }

    public Action<ClusterDefinitionTestDto> requestV4() {
        return new ClusterDefinitionRequestAction();
    }
}