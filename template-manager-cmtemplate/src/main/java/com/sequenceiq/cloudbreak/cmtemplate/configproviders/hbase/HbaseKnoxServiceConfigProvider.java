package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

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
public class HbaseKnoxServiceConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String RESTSERVER_SECURITY_AUTHENTICATION = "hbase_restserver_security_authentication";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        return List.of(
                config(RESTSERVER_SECURITY_AUTHENTICATION, "Kerberos"));
    }

    @Override
    public String getServiceType() {
        return "HBASE";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HbaseRoles.HBASERESTSERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().getValue().contains(ExposedService.HBASE.getKnoxService());
    }

}
