package com.sequenceiq.cloudbreak.blueprint.sharedservice;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class SharedServiceConfigsViewProvider {

    private static final String DEFAULT_RANGER_PORT = "6080";

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    public SharedServiceConfigsView createSharedServiceConfigs(Blueprint blueprint, String ambariPassword, Stack dataLakeStack) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        if (dataLakeStack != null) {
            BlueprintTextProcessor blueprintTextProcessor = blueprintProcessorFactory.get(dataLakeStack.getCluster().getBlueprint().getBlueprintText());
            Map<String, Map<String, String>> configurationEntries = blueprintTextProcessor.getConfigurationEntries();
            Map<String, String> rangerAdminConfigs = configurationEntries.getOrDefault("ranger-admin-site", new HashMap<>());
            String rangerPort = rangerAdminConfigs.getOrDefault("ranger.service.http.port", DEFAULT_RANGER_PORT);

            sharedServiceConfigsView.setRangerAdminPassword(dataLakeStack.getCluster().getPassword());
            sharedServiceConfigsView.setAttachedCluster(true);
            sharedServiceConfigsView.setDatalakeCluster(false);
            sharedServiceConfigsView.setRangerAdminPort(rangerPort);
        } else if (isSharedServiceReqdyBlueprint(blueprint)) {
            sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
            sharedServiceConfigsView.setAttachedCluster(false);
            sharedServiceConfigsView.setDatalakeCluster(true);
            sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
        } else {
            sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
            sharedServiceConfigsView.setAttachedCluster(false);
            sharedServiceConfigsView.setDatalakeCluster(false);
            sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
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
