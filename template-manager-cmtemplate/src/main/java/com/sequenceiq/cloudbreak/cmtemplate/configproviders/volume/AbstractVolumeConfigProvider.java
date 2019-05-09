package com.sequenceiq.cloudbreak.cmtemplate.configproviders.volume;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

public abstract class AbstractVolumeConfigProvider extends AbstractRoleConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVolumeConfigProvider.class);

    boolean hasAnyAttachedDisks(HostgroupView hostGroupView) {
        return hostGroupView.getVolumeCount() > 0;
    }
}
