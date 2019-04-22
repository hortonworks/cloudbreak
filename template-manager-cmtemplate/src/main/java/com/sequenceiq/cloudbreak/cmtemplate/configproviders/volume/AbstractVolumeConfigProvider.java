package com.sequenceiq.cloudbreak.cmtemplate.configproviders.volume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

public abstract class AbstractVolumeConfigProvider implements CmTemplateComponentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVolumeConfigProvider.class);

    @Override
    public Map<String, List<ApiClusterTemplateConfig>> getRoleConfigs(CmTemplateProcessor cmTemplate, TemplatePreparationObject templatePreparationObject) {
        Map<String, List<ApiClusterTemplateConfig>> configs = new HashMap<>();

        for (HostgroupView hostGroupView : templatePreparationObject.getHostgroupViews()) {
            for (ApiClusterTemplateHostTemplate hostTemplate : cmTemplate.getTemplate().getHostTemplates()) {
                String hostGroupName = hostTemplate.getRefName();
                if (hostGroupName.equals(hostGroupView.getName())) {
                    for (String roleType : getRoleTypes()) {
                        Optional<String> roleRefOpt = findRoleRef(cmTemplate.getTemplate(), hostTemplate, roleType);
                        if (roleRefOpt.isPresent()) {
                            List<ApiClusterTemplateConfig> roleConfigs = configs.computeIfAbsent(roleRefOpt.get(), v -> new ArrayList<>());
                            List<ApiClusterTemplateConfig> roleConfig = getRoleConfig(roleType, hostGroupView);
                            setConfigs(roleConfigs, roleConfig);
                        }
                    }
                }
            }
        }

        return configs;
    }

    @Override
    public List<ApiClusterTemplateVariable> getRoleConfigVariables(CmTemplateProcessor cmTemplate, TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();

        for (HostgroupView hostGroupView : templatePreparationObject.getHostgroupViews()) {
            for (ApiClusterTemplateHostTemplate hostTemplate : cmTemplate.getTemplate().getHostTemplates()) {
                String hostGroupName = hostTemplate.getRefName();
                if (hostGroupName.equals(hostGroupView.getName())) {
                    for (String roleType : getRoleTypes()) {
                        Optional<String> roleRefOpt = findRoleRef(cmTemplate.getTemplate(), hostTemplate, roleType);
                        if (roleRefOpt.isPresent()) {
                            result.addAll(getVariables(roleType, hostGroupView, templatePreparationObject));
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private void setConfigs(List<ApiClusterTemplateConfig> roleConfigs, List<ApiClusterTemplateConfig> roleConfig) {
        for (ApiClusterTemplateConfig config : roleConfig) {
            Optional<ApiClusterTemplateConfig> existingConfOpt = roleConfigs.stream()
                    .filter(rc -> rc.getName().equals(config.getName())).findAny();
            if (existingConfOpt.isPresent()) {
                ApiClusterTemplateConfig existingConf = existingConfOpt.get();
                existingConf.setValue(null);
                existingConf.setVariable(config.getVariable());
            } else {
                roleConfigs.add(config);
            }
        }
    }

    private Optional<String> findRoleRef(ApiClusterTemplate cmTemplate, ApiClusterTemplateHostTemplate hostTemplate, String roleType) {
        for (String roleConfigGroupsRefName : hostTemplate.getRoleConfigGroupsRefNames()) {
            if (cmTemplate.getServices().stream().filter(srv -> srv.getRoleConfigGroups() != null).flatMap(srv -> srv.getRoleConfigGroups().stream())
                    .anyMatch(roleConfigGroup -> isRefNameAndTypeMatch(roleType, roleConfigGroupsRefName, roleConfigGroup))) {
                return Optional.of(roleConfigGroupsRefName);
            }
        }
        return Optional.empty();
    }

    private boolean isRefNameAndTypeMatch(String roleType, String roleConfigGroupsRefName, ApiClusterTemplateRoleConfigGroup roleConfigGroup) {
        return roleConfigGroup.getRefName().equals(roleConfigGroupsRefName) && roleConfigGroup.getRoleType().equals(roleType);
    }

    abstract List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView);

    abstract List<ApiClusterTemplateVariable> getVariables(String roleType, HostgroupView hostgroupView, TemplatePreparationObject templatePreparationObject);

    String getRoleTypeVariableName(String hostGroup, String roleType, String propertyKey) {
        return String.format("%s_%s_%s", hostGroup, roleType.toLowerCase(), propertyKey);
    }

    boolean hasAnyAttachedDisks(HostgroupView hostGroupView) {
        return hostGroupView.getVolumeCount() > 0;
    }
}
