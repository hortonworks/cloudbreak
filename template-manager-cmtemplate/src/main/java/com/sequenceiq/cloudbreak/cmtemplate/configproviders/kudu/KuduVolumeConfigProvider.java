package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathFromVolumeIndexZeroVolumeHandled;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class KuduVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    private static final String KUDU_FS_WAL_DIRS = "fs_wal_dir";

    private static final String KUDU_FS_DATA_DIRS = "fs_data_dirs";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {

        switch (roleType) {
            case KuduRoles.KUDU_MASTER :
            case KuduRoles.KUDU_TSERVER:
                String directorySuffix = KuduRoles.KUDU_MASTER.equals(roleType)  ? "kudu/master" : "kudu/tserver";
                //Only one volume needs to be configured for KUDU_FS_WAL_DIRS
                Integer walVolumeCount = hostGroupView.getVolumeCount() > 0 ? 1 : 0;
                Integer dataDirVolumeIndex = hostGroupView.getVolumeCount() <= 1 ? 1 : 2;
                return List.of(
                        config(KUDU_FS_WAL_DIRS, buildVolumePathStringZeroVolumeHandled(walVolumeCount, directorySuffix)),
                        config(KUDU_FS_DATA_DIRS,
                                buildVolumePathFromVolumeIndexZeroVolumeHandled(dataDirVolumeIndex, hostGroupView.getVolumeCount(), directorySuffix))
                        );
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return KuduRoles.KUDU_SERVICE;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(KuduRoles.KUDU_MASTER, KuduRoles.KUDU_TSERVER);
    }
}
