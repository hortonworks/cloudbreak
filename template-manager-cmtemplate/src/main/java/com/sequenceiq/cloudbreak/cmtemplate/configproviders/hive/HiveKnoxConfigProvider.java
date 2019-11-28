package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
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

    static final String HIVE_SERVICE_CONFIG_SAFETY_VALVE = "hive_service_config_safety_valve";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        String filePerEvent = ConfigUtils.getSafetyValveProperty("hive.hook.proto.file.per.event", "true");

        String cdhVersion = templateProcessor.getVersion().orElse("");
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_0)) {
            return List.of(config(HIVE_SERVICE_CONFIG_SAFETY_VALVE, filePerEvent));
        } else {
            KerberosConfig kerberosConfigOpt = templatePreparationObject.getKerberosConfig().get();
            String realm = kerberosConfigOpt.getRealm();
            String principal = ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.principal", "HTTP/_HOST@" + realm);
            String keytab = ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.keytab", "hive.keytab");
            return List.of(config(HIVE_SERVICE_CONFIG_SAFETY_VALVE, principal + keytab + filePerEvent));
        }
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
