package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class RangerUserSyncRoleConfigProvider extends AbstractRoleConfigProvider {
    private static final String ROLE_ASSIGNMENT_RULES = "ranger.usersync.group.based.role.assignment.rules";

    private static final String ROLE_SAFETY_VALVE = "conf/ranger-ugsync-site.xml_role_safety_valve";

    private static final String RANGER_USERSYNC_UNIX_BACKEND = "ranger.usersync.unix.backend";

    @Inject
    private VirtualGroupService virtualGroupService;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        String adminGroup = virtualGroupService.getVirtualGroup(source.getVirtualGroupRequest(), UmsRight.RANGER_ADMIN.getRight());
        return List.of(
                config(ROLE_SAFETY_VALVE, ConfigUtils.getSafetyValveProperty(RANGER_USERSYNC_UNIX_BACKEND, "nss")),
                config(ROLE_ASSIGNMENT_RULES, "&ROLE_SYS_ADMIN:g:" + adminGroup));
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
