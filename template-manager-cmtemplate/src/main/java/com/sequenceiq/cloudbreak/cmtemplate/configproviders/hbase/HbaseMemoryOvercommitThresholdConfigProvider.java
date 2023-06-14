package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_17;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HbaseMemoryOvercommitThresholdConfigProvider implements CmHostGroupRoleConfigProvider {

    private static final String MEMORY_OVERCOMMIT_THRESHOLD = "memory_overcommit_threshold";

    private static final String MEMORY_OVERCOMMIT_THRESHOLD_DEFAULT_VALUE = "0.9";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType,
        HostgroupView hostGroupView, TemplatePreparationObject source) {
        String cdpVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
            "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdpVersion, CLOUDERA_STACK_VERSION_7_2_17)) {
            return List.of(config(MEMORY_OVERCOMMIT_THRESHOLD,
                MEMORY_OVERCOMMIT_THRESHOLD_DEFAULT_VALUE));
        }
        return List.of();
    }

    @Override
    public String getServiceType() {
        return HbaseRoles.HBASE;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(HbaseRoles.REGIONSERVER);
    }
}

