package com.sequenceiq.cloudbreak.blueprint.llap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;

@Component
public class LlapConfigProvider implements BlueprintComponentConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LlapConfigProvider.class);

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Override
    public String configure(BlueprintPreparationObject source, String blueprintText) {
        LOGGER.info("Hive server interactive exists in Blueprint");
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry("hive-interactive-env", "num_llap_nodes",
                String.valueOf(source.getStack().getFullNodeCount() - 1)));
        return blueprintProcessor.addConfigEntries(blueprintText, configs, false);
    }

    @Override
    public Set<String> components() {
        return Sets.newHashSet("HIVE_SERVER_INTERACTIVE");
    }
}
