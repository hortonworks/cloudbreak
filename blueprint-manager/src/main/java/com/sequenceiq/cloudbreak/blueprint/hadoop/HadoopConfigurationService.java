package com.sequenceiq.cloudbreak.blueprint.hadoop;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.ConfigService;
import com.sequenceiq.cloudbreak.blueprint.HdfClusterLocator;
import com.sequenceiq.cloudbreak.domain.HostGroup;

@Service
public class HadoopConfigurationService implements BlueprintComponentConfigProvider {

    @Inject
    private HdfClusterLocator hdfClusterLocator;

    @Inject
    private ConfigService configService;

    @Override
    public String configure(BlueprintPreparationObject source, String blueprintText) throws IOException {
        Map<String, Map<String, Map<String, String>>> hostGroupConfig = configService.getHostGroupConfiguration(blueprintText, source.getHostGroups());
        blueprintText = source.getAmbariClient().extendBlueprintHostGroupConfiguration(blueprintText, hostGroupConfig);

        Map<String, Map<String, String>> globalConfig = getGlobalConfiguration(blueprintText, source.getHostGroups());
        return source.getAmbariClient().extendBlueprintGlobalConfiguration(blueprintText, globalConfig);
    }

    @Override
    public boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return !hdfClusterLocator.hdfCluster(source.getStackRepoDetails());
    }

    private Map<String, Map<String, String>> getGlobalConfiguration(String blueprintText, Collection<HostGroup> hostGroups) {
        Map<String, Map<String, String>> config = configService.getComponentsByHostGroup(blueprintText, hostGroups);
        configService.collectBlueprintConfigIfNeed(config);
        return config;
    }
}
