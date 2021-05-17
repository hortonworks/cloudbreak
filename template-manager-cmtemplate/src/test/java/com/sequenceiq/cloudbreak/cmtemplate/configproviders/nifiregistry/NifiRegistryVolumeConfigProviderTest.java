package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifiregistry;

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
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

public class NifiRegistryVolumeConfigProviderTest {

    private final NifiRegistryVolumeConfigProvider subject = new NifiRegistryVolumeConfigProvider();

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
    void testRoleConfigsWithOneVolume() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);

        assertEquals(List.of(
                config("log_dir", "/hadoopfs/fs1/nifi-registry-log")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithOneVolumeAndStackVersion7210() {
        when(cmTemplateProcessorMock.getStackVersion()).thenReturn("7.2.10");
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);

        assertEquals(List.of(
                config("log_dir", "/hadoopfs/fs1/nifi-registry-log"),
                config("nifi.registry.working.directory", "/hadoopfs/fs1/working-dir")),
                subject.getRoleConfigs(NifiRoles.NIFI_NODE, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    @Test
    void testRoleConfigsWithOneVolumeAndStackVersion7211() {
        when(cmTemplateProcessorMock.getStackVersion()).thenReturn("7.2.11");
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);

        assertEquals(List.of(
                config("log_dir", "/hadoopfs/fs1/nifi-registry-log"),
                config("nifi.registry.working.directory", "/hadoopfs/fs1/working-dir")),
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

