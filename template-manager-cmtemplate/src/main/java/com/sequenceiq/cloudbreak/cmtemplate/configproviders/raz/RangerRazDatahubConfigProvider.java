package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

/**
 * Enables the Ranger Raz service.
 */
@Component
public class RangerRazDatahubConfigProvider extends RangerRazBaseConfigProvider {

    private static final int MIN_ZK_SERVER_COUNT = 2;

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.WORKLOAD == source.getStackType()
                && (CloudPlatform.AWS == source.getCloudPlatform() || CloudPlatform.AZURE == source.getCloudPlatform())
                && source.getProductDetailsView() != null
                && source.getProductDetailsView().getCm() != null
                && CMRepositoryVersionUtil.isRazConfigurationSupportedInDatahub(source.getProductDetailsView().getCm())
                && source.getDatalakeView().isPresent()
                && source.getDatalakeView().get().isRazEnabled();
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (isConfigurationNeeded(cmTemplateProcessor, source)) {
            ApiClusterTemplateService coreSettings = createTemplate();
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();
            Map<String, Set<ServiceComponent>> serviceComponentsByHostGroup = cmTemplateProcessor.getServiceComponentsByHostGroup();
            Set<String> zkServerGroups = collectZKServers(serviceComponentsByHostGroup);
            boolean weHaveMoreThan2ZKServer = getZKHostNumbers(hostgroupViews, zkServerGroups);

            return hostgroupViews.stream()
                    .filter(hg -> isProperHostGroupForRaz(hg, zkServerGroups, weHaveMoreThan2ZKServer))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> coreSettings));
        }
        return Map.of();
    }

    private Set<String> collectZKServers(Map<String, Set<ServiceComponent>> serviceComponentsByHostGroup) {
        return serviceComponentsByHostGroup.entrySet()
                .stream()
                .filter(hg -> hg.getValue().stream().findFirst().isPresent())
                .filter(e -> e.getValue().stream()
                        .filter(services -> services.getService().equals("ZOOKEEPER") && services.getComponent().equals("SERVER"))
                        .findFirst()
                        .isPresent()
                )
                .map(e -> e.getKey())
                .collect(Collectors.toSet());
    }

    private boolean getZKHostNumbers(Set<HostgroupView> hostgroupViews, Set<String> zkServerGroups) {
        Set<HostgroupView> groupsWhichHasZK = hostgroupViews.stream()
                .filter(hg -> zkServerGroups.contains(hg.getName()))
                .collect(Collectors.toSet());
        int numOfZKServers = groupsWhichHasZK.stream().mapToInt(HostgroupView::getNodeCount).sum();
        return numOfZKServers >= MIN_ZK_SERVER_COUNT ? true : false;
    }

    private boolean isProperHostGroupForRaz(HostgroupView hg, Set<String> zookeeperGroups, boolean weHaveMoreThan2ZKServer) {
        return InstanceGroupType.GATEWAY.equals(hg.getInstanceGroupType())
                || (weHaveMoreThan2ZKServer && zookeeperGroups.contains(hg.getName()));
    }
}
