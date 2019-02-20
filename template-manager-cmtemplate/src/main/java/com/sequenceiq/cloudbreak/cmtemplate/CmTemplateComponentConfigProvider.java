package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.List;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

public interface CmTemplateComponentConfigProvider {

    String getServiceType();

    String getRoleType();

    default boolean specialCondition(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return false;
    }

    default List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        return List.of();
    }

    default List<ApiClusterTemplateVariable> getVariables(TemplatePreparationObject templatePreparationObject) {
        return List.of();
    }

}
