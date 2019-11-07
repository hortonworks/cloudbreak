package com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin.ZeppelinRoles.ZEPPELIN_SERVER;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class ZeppelinRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String ZEPPELIN_ADMIN_GROUP = "zeppelin.admin.group";

    @Inject
    private VirtualGroupService virtualGroupService;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject templatePreparationObject) {
        String adminGroup = virtualGroupService.getVirtualGroup(templatePreparationObject.getVirtualGroupRequest(), UmsRight.ZEPPELIN_ADMIN.getRight());
        return List.of(config(ZEPPELIN_ADMIN_GROUP, adminGroup));
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
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
