package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

/**
 * Generates host group-specific role config.
 */
public interface CmHostGroupRoleConfigProvider {

    String getServiceType();

    Set<String> getRoleTypes();

    List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source);

    default boolean sharedRoleType(String roleType) {
        return true;
    }

    default Map<String, String> getConfigAfterAddingVolumes(HostgroupView hostgroupView, TemplatePreparationObject source,
        ServiceComponent serviceComponent) {
        return Map.of();
    }

}
