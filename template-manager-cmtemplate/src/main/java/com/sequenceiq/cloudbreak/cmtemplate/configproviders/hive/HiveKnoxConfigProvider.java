package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

@Component
public class HiveKnoxConfigProvider implements CmTemplateComponentConfigProvider {

    static final String HIVE_SERVICE_CONFIG_SAFETY_VALVE = "hive_service_config_safety_valve";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();
        String filePerEvent = ConfigUtils.getSafetyValveProperty("hive.hook.proto.file.per.event", "true");
        StringBuilder hiveServiceConfigSafetyValveValue = new StringBuilder();
        String cdhVersion = templateProcessor.getVersion().orElse("");
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_0)) {
            hiveServiceConfigSafetyValveValue.append(filePerEvent);
        } else {
            KerberosConfig kerberosConfig = templatePreparationObject.getKerberosConfig().get();
            String realm = kerberosConfig.getRealm();
            String principal = ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.principal", "HTTP/_HOST@" + realm);
            String keytab = ConfigUtils.getSafetyValveProperty("hive.server2.authentication.spnego.keytab", "hive.keytab");
            hiveServiceConfigSafetyValveValue.append(principal + keytab + filePerEvent);
        }
        if (CMRepositoryVersionUtil.isS3SslChannelModeSupported(cdhVersion, templatePreparationObject.getCloudPlatform(),
                templatePreparationObject.getPlatformVariant())) {
            String sslChannelMode = ConfigUtils.getSafetyValveProperty("fs.s3a.ssl.channel.mode", "openssl");
            hiveServiceConfigSafetyValveValue.append(sslChannelMode);
        }
        setupRemoteHmsIfNeeded(templateProcessor, templatePreparationObject, hiveServiceConfigSafetyValveValue);
        if (!hiveServiceConfigSafetyValveValue.toString().isEmpty()) {
            serviceConfigs.add(config(HIVE_SERVICE_CONFIG_SAFETY_VALVE, hiveServiceConfigSafetyValveValue.toString()));
        }
        return serviceConfigs;
    }

    /**
     * If there is no DH HMS, point to DL HMS - TODO remove after OPSAPS-73356
     */
    private void setupRemoteHmsIfNeeded(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject,
            StringBuilder hiveServiceConfigSafetyValveValue) {
        Optional<DatalakeView> datalakeView = templatePreparationObject.getDatalakeView();
        if (StackType.WORKLOAD.equals(templatePreparationObject.getStackType())
                && datalakeView.isPresent()
                && !templateProcessor.isRoleTypePresentInService(HiveRoles.HIVE, List.of(HiveRoles.HIVEMETASTORE))) {

            Set<String> hmsUris = datalakeView.get().getRdcView().getEndpoints(HiveRoles.HIVE, HiveRoles.HIVEMETASTORE);
            if (!hmsUris.isEmpty()) {
                hiveServiceConfigSafetyValveValue.append(ConfigUtils.getSafetyValveProperty("hive.metastore.uris", Joiner.on(',').join(hmsUris)));
            }
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
