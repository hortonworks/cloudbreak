package com.sequenceiq.cloudbreak.clusterdefinition.hbase;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionComponentConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;

@Component
public class HbaseConfigProvider implements ClusterDefinitionComponentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HbaseConfigProvider.class);

    @Override
    public AmbariBlueprintTextProcessor customTextManipulation(TemplatePreparationObject source, AmbariBlueprintTextProcessor blueprintProcessor) {
        Set<String> hbaseMasters = blueprintProcessor.getHostGroupsWithComponent("HBASE_MASTER");
        Set<String> hbaseClients = blueprintProcessor.getHostGroupsWithComponent("HBASE_CLIENT");

        Set<String> clientMissingInTheseGroups = new HashSet<>();
        for (String hbaseMasterGroup : hbaseMasters) {
            if (!hbaseClients.contains(hbaseMasterGroup)) {
                clientMissingInTheseGroups.add(hbaseMasterGroup);
            }
        }
        LOGGER.debug("These groups have missing 'HBASE_CLIENT' component: {}", clientMissingInTheseGroups);
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
