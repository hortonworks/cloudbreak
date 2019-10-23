package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HiveKnoxConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String HIVE_SERVICE_CONFIG_SAFETY_VALVE = "hive_service_config_safety_valve";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        KerberosConfig kerberosConfigOpt = templatePreparationObject.getKerberosConfig().get();
        String realm = kerberosConfigOpt.getRealm();
        String keytab = ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.keytab", "hive.keytab");
        String principal = ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.principal", "HTTP/_HOST@" + realm);
        String filePerEvent = ConfigUtils.getSafetyValveProperty("hive.hook.proto.file.per.event", "true");
        return List.of(config(HIVE_SERVICE_CONFIG_SAFETY_VALVE,
                principal + keytab + filePerEvent));
    }

    @Override
    public String getServiceType() {
        return HiveRoles.HIVE_ON_TEZ;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HiveRoles.HIVESERVER2);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes()) &&
                source.getKerberosConfig().isPresent();
    }
}
