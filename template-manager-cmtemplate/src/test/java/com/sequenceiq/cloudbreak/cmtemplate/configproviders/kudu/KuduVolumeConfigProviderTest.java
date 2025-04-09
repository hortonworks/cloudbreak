package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeCount;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class KuduVolumeConfigProviderTest {

    private final KuduVolumeConfigProvider subject = new KuduVolumeConfigProvider();

    @Test
    void testRoleConfigsWithMultipleVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(3);

        assertEquals(List.of(
                config("fs_wal_dir", "/hadoopfs/fs1/kudu/master"),
                config("fs_data_dirs", "/hadoopfs/fs2/kudu/master,/hadoopfs/fs3/kudu/master")),
                subject.getRoleConfigs(KuduRoles.KUDU_MASTER, hostGroup, getTemplatePreparationObject(hostGroup))
        );

        assertEquals(List.of(
                config("fs_wal_dir", "/hadoopfs/fs1/kudu/tserver"),
                config("fs_data_dirs", "/hadoopfs/fs2/kudu/tserver,/hadoopfs/fs3/kudu/tserver")),
                subject.getRoleConfigs(KuduRoles.KUDU_TSERVER, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithSingleVolume() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);

        assertEquals(List.of(
                config("fs_wal_dir", "/hadoopfs/fs1/kudu/master"),
                config("fs_data_dirs", "/hadoopfs/fs1/kudu/master")),
                subject.getRoleConfigs(KuduRoles.KUDU_MASTER, hostGroup, getTemplatePreparationObject(hostGroup))
        );

        assertEquals(List.of(
                config("fs_wal_dir", "/hadoopfs/fs1/kudu/tserver"),
                config("fs_data_dirs", "/hadoopfs/fs1/kudu/tserver")),
                subject.getRoleConfigs(KuduRoles.KUDU_TSERVER, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithoutVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(0);

        assertEquals(List.of(
                config("fs_wal_dir", "/hadoopfs/root1/kudu/master"),
                config("fs_data_dirs", "/hadoopfs/root1/kudu/master")),
                subject.getRoleConfigs(KuduRoles.KUDU_MASTER, hostGroup, getTemplatePreparationObject(hostGroup))
        );

        assertEquals(List.of(
                config("fs_wal_dir", "/hadoopfs/root1/kudu/tserver"),
                config("fs_data_dirs", "/hadoopfs/root1/kudu/tserver")),
                subject.getRoleConfigs(KuduRoles.KUDU_TSERVER, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    private TemplatePreparationObject getTemplatePreparationObject(HostgroupView hostGroup) {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/cdp-data-mart.bp");
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(hostGroup))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, new CmTemplateProcessor(inputJson)))
                .build();
        return preparationObject;
    }
}
