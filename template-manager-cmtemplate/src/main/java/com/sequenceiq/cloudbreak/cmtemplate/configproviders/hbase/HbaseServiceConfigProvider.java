package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_6_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HbaseServiceConfigProvider implements CmTemplateComponentConfigProvider {
    private static final String SPNEGO_ADMIN_GROUP = "hbase_security_authentication_spnego_admin_groups";
    private static final String RANGER_HBASE_ADMIN_VIRTUAL_GROUPS = "ranger.hbase.default.admin.groups";

    @Inject
    private VirtualGroupService virtualGroupService;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();
        String cmVersion = templateProcessor.getCmVersion().orElse("");
        VirtualGroupRequest virtualGroupRequest = source.getVirtualGroupRequest();
        String adminGroup = virtualGroupService.getVirtualGroup(virtualGroupRequest, UmsRight.HBASE_ADMIN.getRight());
        if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_0)) {
            configList.add(config(SPNEGO_ADMIN_GROUP, adminGroup));
        }
        if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_6_0)) {
            configList.add(config(RANGER_HBASE_ADMIN_VIRTUAL_GROUPS, adminGroup));
        }
        return configList;
    }

    @Override
    public String getServiceType() {
        return HbaseRoles.HBASE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HbaseRoles.MASTER, HbaseRoles.REGIONSERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
