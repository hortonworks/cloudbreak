package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Set;

import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerUpgradeParcelDistributeListenerTask extends AbstractClouderaManagerParcelListenerTask {

    private static final Set<ParcelStatus> DISTRIBUTED_STATUSES = Set.of(
            ParcelStatus.DISTRIBUTED,
            ParcelStatus.ACTIVATED,
            ParcelStatus.ACTIVATING
    );

    private final ParcelResource parcelResource;

    public ClouderaManagerUpgradeParcelDistributeListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, ParcelResource parcelResource) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.parcelResource = parcelResource;
    }

    @Override
    protected String getClusterName(ClouderaManagerCommandPollerObject pollerObject) {
        return parcelResource.getClusterName();
    }

    @Override
    protected String getProduct() {
        return parcelResource.getProduct();
    }

    @Override
    protected String getVersion() {
        return parcelResource.getVersion();
    }

    @Override
    protected Set<ParcelStatus> getExpectedParcelStatuses() {
        return DISTRIBUTED_STATUSES;
    }

    @Override
    protected String getCommandName() {
        return "Distribute CDP Runtime parcel for upgrade.";
    }
}
