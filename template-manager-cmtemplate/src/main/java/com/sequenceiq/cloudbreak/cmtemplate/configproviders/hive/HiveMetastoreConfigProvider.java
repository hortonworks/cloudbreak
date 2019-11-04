package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class HiveMetastoreConfigProvider extends AbstractRdsRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView hiveView = getRdsView(source);

        List<ApiClusterTemplateConfig> configs = Lists.newArrayList(
                config("hive_metastore_database_host", hiveView.getHost()),
                config("hive_metastore_database_name", hiveView.getDatabaseName()),
                config("hive_metastore_database_password", hiveView.getConnectionPassword()),
                config("hive_metastore_database_port", hiveView.getPort()),
                config("hive_metastore_database_type", hiveView.getSubprotocol()),
                config("hive_metastore_database_user", hiveView.getConnectionUserName())
        );

        Optional<KerberosConfig> kerberosConfigOpt = source.getKerberosConfig();
        if (kerberosConfigOpt.isPresent()) {
            String safetyValveValue = "<property><name>hive.hook.proto.file.per.event</name><value>true</value></property>";
            configs.add(config("hive_service_config_safety_valve", safetyValveValue));
        }

        if (source.getStackType() == StackType.DATALAKE) {
            source.getLdapConfig().ifPresent(ldap -> {
                configs.add(config("hive_metastore_enable_ldap_auth", "true"));
                configs.add(config("hive_metastore_ldap_uri", ldap.getConnectionURL()));
                configs.add(config("hive_metastore_ldap_basedn", ldap.getUserSearchBase()));
            });
        }
        return configs;
    }

    @Override
    public String getServiceType() {
        return HiveRoles.HIVE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HiveRoles.HIVEMETASTORE);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.HIVE;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        return List.of(
                config("metastore_canary_health_enabled", Boolean.FALSE.toString())
        );
    }

}
