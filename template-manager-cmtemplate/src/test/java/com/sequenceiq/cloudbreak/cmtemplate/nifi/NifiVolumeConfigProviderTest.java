package com.sequenceiq.cloudbreak.cmtemplate.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeCount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiVolumeConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

public class NifiVolumeConfigProviderTest {

    private final NifiVolumeConfigProvider subject = new NifiVolumeConfigProvider();

    @Mock
    private CmTemplateProcessor cmTemplateProcessorMock;

    @Mock
    private BlueprintView blueprintViewMock;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(blueprintViewMock.getProcessor()).thenReturn(cmTemplateProcessorMock);
        when(cmTemplateProcessorMock.getStackVersion()).thenReturn("7.2.9");
    }

    @Test
    void testRoleConfigsWithFourVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(4);

        assertEquals(List.of(
                config("nifi.flowfile.repository.directory", "/hadoopfs/fs1/flowfile-repo"),
                config("nifi.content.repository.directory.default", "/hadoopfs/fs2/content-repo"),
                config("nifi.provenance.repository.directory.default", "/hadoopfs/fs3/provenance-repo"),
                config("log_dir", "/hadoopfs/fs4/nifi-log"),
                config("nifi.database.directory", "/hadoopfs/fs4/database-dir")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithThreeVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(3);

        assertEquals(List.of(
                config("nifi.flowfile.repository.directory", "/hadoopfs/fs1/flowfile-repo"),
                config("nifi.content.repository.directory.default", "/hadoopfs/fs3/content-repo"),
                config("nifi.provenance.repository.directory.default", "/hadoopfs/fs2/provenance-repo"),
                config("log_dir", "/hadoopfs/fs3/nifi-log"),
                config("nifi.database.directory", "/hadoopfs/fs1/database-dir")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithTwoVolumes() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(2);

        assertEquals(List.of(
                config("nifi.flowfile.repository.directory", "/hadoopfs/fs1/flowfile-repo"),
                config("nifi.content.repository.directory.default", "/hadoopfs/fs2/content-repo"),
                config("nifi.provenance.repository.directory.default", "/hadoopfs/fs1/provenance-repo"),
                config("log_dir", "/hadoopfs/fs2/nifi-log"),
                config("nifi.database.directory", "/hadoopfs/fs1/database-dir")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithFourVolumesAndStackVersion7210() {
        when(cmTemplateProcessorMock.getStackVersion()).thenReturn("7.2.10");
        HostgroupView hostGroup = hostGroupWithVolumeCount(4);

        assertEquals(List.of(
                config("nifi.flowfile.repository.directory", "/hadoopfs/fs1/flowfile-repo"),
                config("nifi.content.repository.directory.default", "/hadoopfs/fs2/content-repo"),
                config("nifi.provenance.repository.directory.default", "/hadoopfs/fs3/provenance-repo"),
                config("log_dir", "/hadoopfs/fs4/nifi-log"),
                config("nifi.database.directory", "/hadoopfs/fs4/database-dir"),
                config("nifi.working.directory", "/hadoopfs/fs4/working-dir")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithThreeVolumesAndStackVersion7210() {
        when(cmTemplateProcessorMock.getStackVersion()).thenReturn("7.2.10");
        HostgroupView hostGroup = hostGroupWithVolumeCount(3);

        assertEquals(List.of(
                config("nifi.flowfile.repository.directory", "/hadoopfs/fs1/flowfile-repo"),
                config("nifi.content.repository.directory.default", "/hadoopfs/fs3/content-repo"),
                config("nifi.provenance.repository.directory.default", "/hadoopfs/fs2/provenance-repo"),
                config("log_dir", "/hadoopfs/fs3/nifi-log"),
                config("nifi.database.directory", "/hadoopfs/fs1/database-dir"),
                config("nifi.working.directory", "/hadoopfs/fs3/working-dir")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithTwoVolumesAndStackVersion7210() {
        when(cmTemplateProcessorMock.getStackVersion()).thenReturn("7.2.10");
        HostgroupView hostGroup = hostGroupWithVolumeCount(2);

        assertEquals(List.of(
                config("nifi.flowfile.repository.directory", "/hadoopfs/fs1/flowfile-repo"),
                config("nifi.content.repository.directory.default", "/hadoopfs/fs2/content-repo"),
                config("nifi.provenance.repository.directory.default", "/hadoopfs/fs1/provenance-repo"),
                config("log_dir", "/hadoopfs/fs2/nifi-log"),
                config("nifi.database.directory", "/hadoopfs/fs1/database-dir"),
                config("nifi.working.directory", "/hadoopfs/fs2/working-dir")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithFourVolumesAndStackVersion7211() {
        when(cmTemplateProcessorMock.getStackVersion()).thenReturn("7.2.11");
        HostgroupView hostGroup = hostGroupWithVolumeCount(4);

        assertEquals(List.of(
                config("nifi.flowfile.repository.directory", "/hadoopfs/fs1/flowfile-repo"),
                config("nifi.content.repository.directory.default", "/hadoopfs/fs2/content-repo"),
                config("nifi.provenance.repository.directory.default", "/hadoopfs/fs3/provenance-repo"),
                config("log_dir", "/hadoopfs/fs4/nifi-log"),
                config("nifi.database.directory", "/hadoopfs/fs4/database-dir"),
                config("nifi.working.directory", "/hadoopfs/fs4/working-dir")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithThreeVolumesAndStackVersion7211() {
        when(cmTemplateProcessorMock.getStackVersion()).thenReturn("7.2.11");
        HostgroupView hostGroup = hostGroupWithVolumeCount(3);

        assertEquals(List.of(
                config("nifi.flowfile.repository.directory", "/hadoopfs/fs1/flowfile-repo"),
                config("nifi.content.repository.directory.default", "/hadoopfs/fs3/content-repo"),
                config("nifi.provenance.repository.directory.default", "/hadoopfs/fs2/provenance-repo"),
                config("log_dir", "/hadoopfs/fs3/nifi-log"),
                config("nifi.database.directory", "/hadoopfs/fs1/database-dir"),
                config("nifi.working.directory", "/hadoopfs/fs3/working-dir")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithTwoVolumesAndStackVersion7211() {
        when(cmTemplateProcessorMock.getStackVersion()).thenReturn("7.2.11");
        HostgroupView hostGroup = hostGroupWithVolumeCount(2);

        assertEquals(List.of(
                config("nifi.flowfile.repository.directory", "/hadoopfs/fs1/flowfile-repo"),
                config("nifi.content.repository.directory.default", "/hadoopfs/fs2/content-repo"),
                config("nifi.provenance.repository.directory.default", "/hadoopfs/fs1/provenance-repo"),
                config("log_dir", "/hadoopfs/fs2/nifi-log"),
                config("nifi.database.directory", "/hadoopfs/fs1/database-dir"),
                config("nifi.working.directory", "/hadoopfs/fs2/working-dir")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    private TemplatePreparationObject getTemplatePreparationObject(HostgroupView hostGroup) {
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(hostGroup))
                .withBlueprintView(blueprintViewMock)
                .build();
        return preparationObject;
    }
}
