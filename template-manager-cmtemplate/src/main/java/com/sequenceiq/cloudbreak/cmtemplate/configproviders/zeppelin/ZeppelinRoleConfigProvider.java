package com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin.ZeppelinRoles.ZEPPELIN_SERVER;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class ZeppelinRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String ZEPPELIN_ADMIN_GROUP = "zeppelin.admin.group";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject templatePreparationObject) {
        String adminGroup = templatePreparationObject.getLdapConfig().map(LdapView::getAdminGroup).get();
        return List.of(
                config(ZEPPELIN_ADMIN_GROUP, adminGroup));
    }

    @Override
    public String getServiceType() {
        return ZeppelinRoles.ZEPPELIN;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(ZEPPELIN_SERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes()) &&
                source.getLdapConfig().isPresent() && StringUtils.isNotEmpty(source.getLdapConfig().get().getAdminGroup());
    }
}