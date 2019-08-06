package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Optional;

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

    private static final String VALUE = ConfigUtils.getSafetyValveProperty("hive.server2.thrift.http.port", "10001")
            + ConfigUtils.getSafetyValveProperty("hive.server2.thrift.http.path", "cliservice")
            + ConfigUtils.getSafetyValveProperty("hive.server2.transport.mode", "http")
            + ConfigUtils.getSafetyValveProperty("hive.server2.allow.user.substitution", "true");

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        Optional<KerberosConfig> kerberosConfigOpt = templatePreparationObject.getKerberosConfig();
        if (kerberosConfigOpt.isPresent()) {
            String realm = Optional.ofNullable(kerberosConfigOpt.get().getRealm()).orElse("").toUpperCase();
            String keytab = ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.keytab", "hive.keytab");
            String principal = ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.principal", "HTTP/_HOST@" + realm);
            return List.of(config(HIVE_SERVICE_CONFIG_SAFETY_VALVE,
                    VALUE
                            + principal
                            + keytab));
        }
        return List.of(config(HIVE_SERVICE_CONFIG_SAFETY_VALVE, VALUE));
    }

    @Override
    public String getServiceType() {
        return HiveRoles.HIVE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HiveRoles.HIVESERVER2);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

}
