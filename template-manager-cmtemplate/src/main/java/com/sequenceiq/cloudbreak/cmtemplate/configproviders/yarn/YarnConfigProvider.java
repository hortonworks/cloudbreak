package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class YarnConfigProvider implements CmTemplateComponentConfigProvider {

    public static final String KNOX_PROXY_USER_SETTINGS = "<property><name>hadoop.proxyuser.knox.groups</name><value>*"
            + "</value></property><property><name>hadoop.proxyuser.knox.hosts</name><value>*</value></property>";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        return List.of(
                config("yarn_core_site_safety_valve",
                        KNOX_PROXY_USER_SETTINGS));
    }

    @Override
    public String getServiceType() {
        return YarnRoles.YARN;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(YarnRoles.YARN);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().getValue().contains(ExposedService.RESOURCEMANAGER_WEB.getKnoxService());
    }

}
