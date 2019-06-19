package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HdfsConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String HADOOP_HTTP_FILTER_INITIALIZERS = "hadoop.http.filter.initializers";

    private static final String CORE_SITE_SAFETY_VALVE = "core_site_safety_valve";

    private static final String VALUE = "org.apache.hadoop.security.HttpCrossOriginFilterInitializer,"
            + "org.apache.hadoop.security.authentication.server.ProxyUserAuthenticationFilterInitializer";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        return List.of(
                config(CORE_SITE_SAFETY_VALVE,
                        ConfigUtils.getSafetyValveProperty(HADOOP_HTTP_FILTER_INITIALIZERS, VALUE)));
    }

    @Override
    public String getServiceType() {
        return HdfsRoles.HDFS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HdfsRoles.NAMENODE, HdfsRoles.DATANODE, HdfsRoles.SECONDARYNAMENODE, HdfsRoles.JOURNALNODE);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().getValue().contains(ExposedService.NAMENODE.getKnoxService());
    }

}
