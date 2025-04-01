package com.sequenceiq.cloudbreak.cmtemplate.configproviders.tez;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreCloudStorageServiceConfigProvider.HMS_METASTORE_EXTERNAL_DIR;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class TezRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String TEZ_CONF_CLIENT_SAFETY_VALVE = "tez-conf/tez-site.xml_client_config_safety_valve";

    private static final String TEZ_LOGGING_PROTO_BASE_DIR_PARAM = "tez.history.logging.proto-base-dir";

    private static final String TEZ_LOGGING_PROTO_BASE_DIR_SUFFIX = "/sys.db";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        switch (roleType) {
            case TezRoles.GATEWAY:
                return ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_EXTERNAL_DIR)
                        .map(location ->
                                location.getValue().replaceAll("/?$", "") + TEZ_LOGGING_PROTO_BASE_DIR_SUFFIX)
                        .map(logDir -> List.of(config(TEZ_CONF_CLIENT_SAFETY_VALVE,
                                ConfigUtils.getSafetyValveProperty(TEZ_LOGGING_PROTO_BASE_DIR_PARAM, logDir))))
                        .orElseGet(List::of);
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return TezRoles.TEZ;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(TezRoles.GATEWAY);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
