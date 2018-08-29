package com.sequenceiq.cloudbreak.blueprint.hbase;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HbaseConfigProvider implements BlueprintComponentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HbaseConfigProvider.class);

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Override
    public BlueprintTextProcessor customTextManipulation(TemplatePreparationObject source, BlueprintTextProcessor blueprintProcessor) {
        Set<String> hbaseMasters = blueprintProcessor.getHostGroupsWithComponent("HBASE_MASTER");
        Set<String> hbaseClients = blueprintProcessor.getHostGroupsWithComponent("HBASE_CLIENT");

        Set<String> clientMissingInTheseGroups = new HashSet<>();
        for (String hbaseMasterGroup : hbaseMasters) {
            if (!hbaseClients.contains(hbaseMasterGroup)) {
                clientMissingInTheseGroups.add(hbaseMasterGroup);
            }
        }
        LOGGER.info("These groups have missing 'HBASE_CLIENT' component: {}", clientMissingInTheseGroups);
        if (!clientMissingInTheseGroups.isEmpty()) {
            blueprintProcessor.addComponentToHostgroups("HBASE_CLIENT", clientMissingInTheseGroups).asText();
        }
        return blueprintProcessor;
    }

    @Override
    public Set<String> components() {
        return Sets.newHashSet("HBASE_MASTER");
    }
}
