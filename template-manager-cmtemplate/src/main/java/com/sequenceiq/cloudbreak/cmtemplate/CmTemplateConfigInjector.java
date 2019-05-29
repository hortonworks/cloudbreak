package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.List;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

public interface CmTemplateConfigInjector {

    default List<ApiClusterTemplateConfig> getServiceConfigs(
            ApiClusterTemplateService service,
            TemplatePreparationObject source
    ) {
        return List.of();
    }

    default List<ApiClusterTemplateConfig> getRoleConfigs(
            ApiClusterTemplateRoleConfigGroup roleConfigGroup,
            ApiClusterTemplateService service,
            TemplatePreparationObject source
    ) {
        return List.of();
    }

    default void addServiceConfigs(
            ApiClusterTemplateService service,
            CmTemplateProcessor processor,
            TemplatePreparationObject source
    ) {
        processor.mergeServiceConfigs(service, getServiceConfigs(service, source));
    }

    default void addRoleConfigs(
            ApiClusterTemplateRoleConfigGroup roleConfigGroup,
            ApiClusterTemplateService service,
            CmTemplateProcessor processor,
            TemplatePreparationObject source
    ) {
        processor.mergeRoleConfigs(roleConfigGroup, getRoleConfigs(roleConfigGroup, service, source));
    }

}
