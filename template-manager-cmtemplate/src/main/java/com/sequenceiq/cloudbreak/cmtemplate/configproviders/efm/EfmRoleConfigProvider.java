package com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class EfmRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    static final String DATABASE_USER = "efm.db.username";

    static final String DATABASE_PASSWORD = "efm.db.password";

    static final String DATABASE_URL = "efm.db.url";

    static final String DRIVER_CLASS = "efm.db.driverClass";

    @Override
    public String dbUserKey() {
        return DATABASE_USER;
    }

    @Override
    public String dbPasswordKey() {
        return DATABASE_PASSWORD;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView efmRdsView = getRdsView(source);
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();

        if (EfmRoles.EFM_SERVER.equals(roleType)) {
            configs.add(config(DATABASE_URL, efmRdsView.getConnectionURL()));
            configs.add(config(DRIVER_CLASS, efmRdsView.getConnectionDriver()));
            configs.add(config(DATABASE_USER, efmRdsView.getConnectionUserName()));
            configs.add(config(DATABASE_PASSWORD, efmRdsView.getConnectionPassword()));
        }

        return configs;
    }

    @Override
    public String getServiceType() {
        return EfmRoles.EFM;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(EfmRoles.EFM_SERVER);
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.EFM;
    }
}
