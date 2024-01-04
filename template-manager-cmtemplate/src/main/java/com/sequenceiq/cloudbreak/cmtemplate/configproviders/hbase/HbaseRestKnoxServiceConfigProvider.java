package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HbaseRestKnoxServiceConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String RESTSERVER_SECURITY_AUTHENTICATION = "hbase_restserver_security_authentication";

    private static final String HBASE_RPC_PROTECTION = "hbase_rpc_protection";

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        configs.add(config(RESTSERVER_SECURITY_AUTHENTICATION, "kerberos"));
        if (isWireEncryptionEnabled(templatePreparationObject)) {
            configs.add(config(HBASE_RPC_PROTECTION, "privacy"));
        }
        return configs;
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
                && source.getGatewayView().getExposedServices().contains(exposedServiceCollector.getHBaseRestService().getKnoxService());
    }

    private boolean isWireEncryptionEnabled(TemplatePreparationObject source) {
        return entitlementService.isWireEncryptionEnabled(ThreadBasedUserCrnProvider.getAccountId())
                && !CloudPlatform.YARN.equals(source.getCloudPlatform())
                && StackType.DATALAKE.equals(source.getStackType());
    }

}
