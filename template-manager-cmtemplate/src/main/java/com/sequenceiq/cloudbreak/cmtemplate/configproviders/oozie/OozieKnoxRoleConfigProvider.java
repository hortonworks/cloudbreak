package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class OozieKnoxRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String OOZIE_SERVICE_PROXY_USER_SERVICE_PROXYUSER_KNOX_GROUPS = "oozie.service.ProxyUserService.proxyuser.knox.groups";

    private static final String OOZIE_SERVICE_PROXY_USER_SERVICE_PROXYUSER_KNOX_HOSTS = "oozie.service.ProxyUserService.proxyuser.knox.hosts";

    private static final String OOZIE_CONFIG_SAFETY_VALVE = "oozie_config_safety_valve";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        return List.of(
                config(OOZIE_CONFIG_SAFETY_VALVE,
                        ConfigUtils.getSafetyValveProperty(OOZIE_SERVICE_PROXY_USER_SERVICE_PROXYUSER_KNOX_GROUPS, "*")
                                + ConfigUtils.getSafetyValveProperty(OOZIE_SERVICE_PROXY_USER_SERVICE_PROXYUSER_KNOX_HOSTS, "*")));
    }

    @Override
    public String getServiceType() {
        return OozieRoles.OOZIE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(OozieRoles.OOZIE_SERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().contains(ExposedService.OOZIE_UI.getKnoxService());
    }

}
