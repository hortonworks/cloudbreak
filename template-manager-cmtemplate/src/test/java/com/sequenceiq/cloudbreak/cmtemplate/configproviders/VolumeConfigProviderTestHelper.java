package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class VolumeConfigProviderTestHelper {

    private VolumeConfigProviderTestHelper() { }

    public static HostgroupView hostGroupWithVolumeCount(int volumeCount) {
        return new HostgroupView("some name", volumeCount, InstanceGroupType.CORE, 2);
    }

    public static HostgroupView hostGroupWithVolumeTemplates(int volumeCount, Set<VolumeTemplate> volumeTemplates) {
        return new HostgroupView("some name", volumeCount, InstanceGroupType.CORE, Collections.EMPTY_SET, volumeTemplates);
    }

    public static HostgroupView hostGroupWithVolumeTemplatesAndTemporaryStorage(int volumeCount, Set<VolumeTemplate> volumeTemplates,
            TemporaryStorage temporaryStorage, Integer temporaryStorageVolumeCount) {
        return new HostgroupView("some name", volumeCount, InstanceGroupType.CORE, Collections.EMPTY_SET,
                volumeTemplates, temporaryStorage, temporaryStorageVolumeCount);
    }

    public static TemplatePreparationObject preparatorWithHostGroups(HostgroupView... hostGroups) {
        return TemplatePreparationObject.Builder.builder().withHostgroupViews(Set.of(hostGroups)).build();
    }
}
