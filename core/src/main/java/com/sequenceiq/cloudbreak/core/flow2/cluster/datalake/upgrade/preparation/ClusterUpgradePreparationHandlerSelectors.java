package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation;

import com.sequenceiq.flow.core.FlowEvent;

public enum ClusterUpgradePreparationHandlerSelectors implements FlowEvent {

    PREPARE_PARCEL_SETTINGS_EVENT,
    DOWNLOAD_PARCELS_EVENT,
    DISTRIBUTE_PARCELS_EVENT;

    @Override
    public String event() {
        return name();
    }

}
