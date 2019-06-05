package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class OozieKnoxRoleConfigProvider extends AbstractRoleConfigConfigProvider {

    public static final String OOZIE_PROXY_USER_SETTINGS = "<property><name>oozie.service.ProxyUserService.proxyuser.knox"
            + ".groups</name><value>*</value></property><property><name>oozie.service.ProxyUserService.proxyuser.knox"
            + ".hosts</name><value>*</value></property>";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        return List.of(
                config("oozie_config_safety_valve",
                        OOZIE_PROXY_USER_SETTINGS));
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
                && source.getGatewayView().getExposedServices().getValue().contains(ExposedService.OOZIE_UI.getKnoxService());
    }

}
