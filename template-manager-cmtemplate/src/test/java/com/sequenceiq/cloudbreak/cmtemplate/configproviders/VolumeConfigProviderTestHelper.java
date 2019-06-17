package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

public class VolumeConfigProviderTestHelper {

    private VolumeConfigProviderTestHelper() { }

    public static HostgroupView hostGroupWithVolumeCount(int volumeCount) {
        return new HostgroupView("some name", volumeCount, InstanceGroupType.CORE, 2);
    }

    public static TemplatePreparationObject preparatorWithHostGroups(HostgroupView... hostGroups) {
        return TemplatePreparationObject.Builder.builder().withHostgroupViews(Set.of(hostGroups)).build();
    }
}
