package com.sequenceiq.it.cloudbreak.newway.action.clusterdefinition;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionTestDto;

public class ClusterDefinitionTestAction {

    private ClusterDefinitionTestAction() {
    }

    public static ClusterDefinitionTestDto getByName(TestContext testContext, ClusterDefinitionTestDto entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().clusterDefinitionV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Action<ClusterDefinitionTestDto> postV4() {
        return new ClusterDefinitionPostAction();
    }

    public static Action<ClusterDefinitionTestDto> listV4() {
        return new ClusterDefinitionGetListAction();
    }

    public static Action<ClusterDefinitionTestDto> getV4() {
        return new ClusterDefinitionGetAction();
    }

    public static Action<ClusterDefinitionTestDto> deleteV4() {
        return new ClusterDefinitionDeleteAction();
    }

    public static Action<ClusterDefinitionTestDto> requestV4() {
        return new ClusterDefinitionRequestAction();
    }
}