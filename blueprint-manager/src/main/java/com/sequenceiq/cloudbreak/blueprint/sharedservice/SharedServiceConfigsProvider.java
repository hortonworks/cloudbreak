package com.sequenceiq.cloudbreak.blueprint.sharedservice;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component
public class SharedServiceConfigsProvider {

    public SharedServiceConfigsView createSharedServiceConfigs(Blueprint blueprint, String ambariPassword, Stack dataLakeStack) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        if (dataLakeStack != null) {
            sharedServiceConfigsView.setRangerAdminPassword(dataLakeStack.getCluster().getPassword());
            sharedServiceConfigsView.setAttachedCluster(true);
            sharedServiceConfigsView.setDatalakeCluster(false);
        } else if (isSharedServiceReqdyBlueprint(blueprint)) {
            sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
            sharedServiceConfigsView.setAttachedCluster(false);
            sharedServiceConfigsView.setDatalakeCluster(true);
        } else {
            sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
            sharedServiceConfigsView.setAttachedCluster(false);
            sharedServiceConfigsView.setDatalakeCluster(false);
        }
        return sharedServiceConfigsView;
    }

    public SharedServiceConfigsView createSharedServiceConfigs(Stack source, Stack dataLakeStack) {
        return createSharedServiceConfigs(source.getCluster().getBlueprint(), source.getCluster().getPassword(), dataLakeStack);
    }

    private boolean isSharedServiceReqdyBlueprint(Blueprint blueprint) {
        return blueprint.getTags() != null && blueprint.getTags().getMap().containsKey("shared_services_ready");
    }
}
