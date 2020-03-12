package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
public class RangerAutoCompleteConfigProvider {
    public void extendServiceConfigs(TemplatePreparationObject source, List<ApiClusterTemplateConfig> configList) {
        Optional<HostgroupView> gateway = source.getHostgroupViews().stream().filter(hg -> InstanceGroupType.GATEWAY.equals(hg.getInstanceGroupType()))
                .findFirst();
        if (source.getGatewayView() != null && source.getSharedServiceConfigs().isPresent() && gateway.isPresent()) {
            configList.add(config("ranger.fqdn", source.getSharedServiceConfigs().get().getDatalakeClusterManagerFqdn()));
            configList.add(config("knox.gateway.fqdn", gateway.get().getHosts().first()));
            configList.add(config("knox.gateway.proxy.api.topology", source.getGatewayView().getTopologyName() + "-api"));
            configList.add(config("ranger.autocomplete.enabled", Boolean.TRUE.toString()));
        }
    }
}
