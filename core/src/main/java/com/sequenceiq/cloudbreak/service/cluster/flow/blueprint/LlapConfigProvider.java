package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;

@Component
public class LlapConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LlapConfigProvider.class);

    private static final double PERCENTAGE_OF_LLAP_NODES = 0.75;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    public String addToBlueprint(Stack stack, String blueprintText) {
        if (blueprintProcessor.componentExistsInBlueprint("HIVE_SERVER_INTERACTIVE", blueprintText)) {
            LOGGER.info("Hive server interactive exists in Blueprint");
            List<BlueprintConfigurationEntry> configs = getConfigs((int) (stack.getFullNodeCount() * PERCENTAGE_OF_LLAP_NODES));
            blueprintText = blueprintProcessor.addConfigEntries(blueprintText, configs, false);
        }
        return blueprintText;
    }

    private List<BlueprintConfigurationEntry> getConfigs(Integer nodeCount) {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry("hive-interactive-env", "num_llap_nodes", nodeCount.toString()));
        configs.add(new BlueprintConfigurationEntry("hive-interactive-env", "num_llap_nodes_for_llap_daemons", nodeCount.toString()));
        return configs;
    }
}
