package com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper.hostGroupWithVolumeCount;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmRoles.EFM;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmVolumeConfigProvider.CM_LOG_DIR;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmVolumeConfigProvider.EFM_LOG_DIR;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.VOLUME_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

public class EfmVolumeConfigProviderTest {

    private static final String EFM_LOG_PATH = VOLUME_PREFIX + "1" + "/" + EFM_LOG_DIR;

    private final EfmVolumeConfigProvider subject = new EfmVolumeConfigProvider();

    @Test
    void testRoleConfigsWithOneVolume() {
        HostgroupView hostGroup = hostGroupWithVolumeCount(1);

        assertEquals(List.of(
            config(CM_LOG_DIR, EFM_LOG_PATH)),
            subject.getRoleConfigs(EFM, hostGroup, getTemplatePreparationObject(hostGroup))
        );
    }

    private TemplatePreparationObject getTemplatePreparationObject(HostgroupView hostGroup) {
        return TemplatePreparationObject.Builder.builder()
            .withHostgroupViews(Set.of(hostGroup))
            .build();
    }
}
