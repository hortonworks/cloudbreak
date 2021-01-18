package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_8;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getCmVersion;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class RangerUserSyncRoleConfigProvider extends AbstractRoleConfigProvider {
    private static final String ROLE_ASSIGNMENT_RULES = "ranger.usersync.group.based.role.assignment.rules";

    private static final String ROLE_SAFETY_VALVE = "conf/ranger-ugsync-site.xml_role_safety_valve";

    private static final String RANGER_USERSYNC_UNIX_BACKEND = "ranger.usersync.unix.backend";

    private static final String RANGER_USERSYNC_AZURE_MAPPING = "ranger_usersync_azure_user_mapping";

    @Inject
    private VirtualGroupService virtualGroupService;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        String adminGroup = virtualGroupService.getVirtualGroup(source.getVirtualGroupRequest(), UmsRight.RANGER_ADMIN.getRight());
        String rangerAdminAsSysAdminConfigProvider = rangerAdminAsSysAdminConfigProvider(source);
        if (CloudPlatform.AZURE.equals(source.getCloudPlatform()) && source.getGeneralClusterConfigs().isEnableRangerRaz()
                && source.getServicePrincipals() != null) {
            String servicePrincipals = getServicePrincipalsString(source);
            return List.of(
                    config(ROLE_SAFETY_VALVE, ConfigUtils.getSafetyValveProperty(RANGER_USERSYNC_UNIX_BACKEND, "nss")),
                    config(ROLE_ASSIGNMENT_RULES, "&ROLE_SYS_ADMIN:g:" + adminGroup + rangerAdminAsSysAdminConfigProvider),
                    config(RANGER_USERSYNC_AZURE_MAPPING, servicePrincipals));
        } else {
            return List.of(
                    config(ROLE_SAFETY_VALVE, ConfigUtils.getSafetyValveProperty(RANGER_USERSYNC_UNIX_BACKEND, "nss")),
                    config(ROLE_ASSIGNMENT_RULES, "&ROLE_SYS_ADMIN:g:" + adminGroup + rangerAdminAsSysAdminConfigProvider));
        }
    }

    private String rangerAdminAsSysAdminConfigProvider(TemplatePreparationObject source) {
        String cmVersion = getCmVersion(source);
        if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_8)) {
            return "&ROLE_SYS_ADMIN:u:ranger";
        }
        return "";
    }

    private String getServicePrincipalsString(TemplatePreparationObject source) {
        if (source.getServicePrincipals().isEmpty()) {
            return "";
        } else {
            return source.getServicePrincipals().entrySet().stream()
                    .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                    .collect(Collectors.joining(";"));
        }
    }

    @Override
    public String getServiceType() {
        return RangerRoles.RANGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(RangerRoles.RANGER_USERSYNC);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().contains(exposedServiceCollector.getRangerService().getKnoxService());
    }
}
