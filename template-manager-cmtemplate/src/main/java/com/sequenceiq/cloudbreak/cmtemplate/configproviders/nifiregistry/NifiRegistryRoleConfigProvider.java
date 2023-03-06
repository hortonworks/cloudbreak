package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifiregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.cfm.CfmUtil.getCfmProduct;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class NifiRegistryRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    public static final Versioned CFM_VERSION_2_2_7_0 = () -> "2.2.7.0";

    static final String DATABASE_USER = "nifi.registry.db.username";

    static final String DATABASE_PASSWORD = "nifi.registry.db.password";

    static final String DATABASE_URL = "nifi.registry.db.url";

    static final String DRIVER_CLASS = "nifi.registry.db.driver.class";

    static final String DRIVER_DIRECTORY = "nifi.registry.db.driver.directory";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_0_1)) {
            switch (roleType) {
                case NifiRegistryRoles.NIFI_REGISTRY_SERVER:
                    return generateDatabaseConfig(source);
                default:
                    return List.of();
            }
        }
        return List.of();
    }

    private List<ApiClusterTemplateConfig> generateDatabaseConfig(TemplatePreparationObject source) {
        RdsView nifiRegistryRdsView = getRdsView(source);
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();

        configs.add(config(DATABASE_URL, nifiRegistryRdsView.getConnectionURL()));
        configs.add(config(DRIVER_CLASS, nifiRegistryRdsView.getConnectionDriver()));
        configs.add(config(DATABASE_USER, nifiRegistryRdsView.getConnectionUserName()));
        configs.add(config(DATABASE_PASSWORD, nifiRegistryRdsView.getConnectionPassword()));

        Optional<ClouderaManagerProduct> cfm = getCfmProduct(source);
        if (cfm.isEmpty() || isVersionOlderThanLimited(cfm.get().getVersion(), CFM_VERSION_2_2_7_0)) {
            configs.add(config(DRIVER_DIRECTORY, "/usr/share/java/"));
        }

        return unmodifiableList(configs);
    }

    @Override
    public String getServiceType() {
        return NifiRegistryRoles.NIFIREGISTRY;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(NifiRegistryRoles.NIFI_REGISTRY_SERVER);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.NIFIREGISTRY;
    }
}
