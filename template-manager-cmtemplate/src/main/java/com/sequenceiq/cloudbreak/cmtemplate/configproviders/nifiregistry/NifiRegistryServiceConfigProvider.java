package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifiregistry;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static java.util.Collections.emptyList;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class NifiRegistryServiceConfigProvider extends AbstractRdsRoleConfigProvider {

    static final String DATABASE_USER = "nifi.registry.db.username";

    static final String DATABASE_PASSWORD = "nifi.registry.db.password";

    static final String DATABASE_URL = "nifi.registry.db.url";

    static final String DRIVER_CLASS = "nifi.registry.db.driver.class";

    static final String DRIVER_DIRECTORY = "nifi.registry.db.driver.directory";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_0_1)) {
            RdsView nifiRegistryRdsView = getRdsView(source);
            return List.of(
                    config(DATABASE_URL, nifiRegistryRdsView.getConnectionURL()),
                    config(DRIVER_DIRECTORY, "/usr/share/java/"),
                    config(DRIVER_CLASS, nifiRegistryRdsView.getConnectionDriver()),
                    config(DATABASE_USER, nifiRegistryRdsView.getConnectionUserName()),
                    config(DATABASE_PASSWORD, nifiRegistryRdsView.getConnectionPassword())
            );
        }
        return emptyList();
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

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        return List.of();
    }
}
