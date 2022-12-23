package com.sequenceiq.cloudbreak.cm.polling.task;

import com.sequenceiq.cloudbreak.cm.model.ParcelResource;

class ClouderaManagerUpgradeParcelDistributeListenerTaskTest
        extends AbstractClouderaManagerParcelListenerTaskTest<ClouderaManagerUpgradeParcelDistributeListenerTask> {

    @Override
    ClouderaManagerUpgradeParcelDistributeListenerTask setUpUnderTest() {
        ParcelResource parcelResource = new ParcelResource(CLUSTER_NAME, PRODUCT, VERSION);
        return new ClouderaManagerUpgradeParcelDistributeListenerTask(clouderaManagerApiPojoFactory, clusterEventService, parcelResource);
    }
}
