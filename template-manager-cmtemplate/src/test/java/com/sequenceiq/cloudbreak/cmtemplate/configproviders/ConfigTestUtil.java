package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;

public class ConfigTestUtil {

    private ConfigTestUtil() {

    }

    public static Map<String, String> getConfigNameToValueMap(List<ApiClusterTemplateConfig> configs) {
        return configs.stream().filter(config -> config.getValue() != null)
                .collect(toMap(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getValue));
    }

    public static Map<String, String> getConfigNameToVariableNameMap(List<ApiClusterTemplateConfig> configs) {
        return configs.stream().filter(config -> config.getVariable() != null)
                .collect(toMap(ApiClusterTemplateConfig::getName, ApiClusterTemplateConfig::getVariable));
    }

    public static Map<String, String> getVariableNameToValueMap(List<ApiClusterTemplateVariable> variables) {
        return variables.stream().collect(toMap(ApiClusterTemplateVariable::getName, ApiClusterTemplateVariable::getValue));
    }

}
