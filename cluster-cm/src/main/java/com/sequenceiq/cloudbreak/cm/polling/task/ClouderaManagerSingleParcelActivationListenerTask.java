package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerSingleParcelActivationListenerTask extends AbstractClouderaManagerParcelListenerTask {

    private static final Set<ParcelStatus> ACTIVATED_STATUSES = Set.of(ParcelStatus.ACTIVATED);

    private final ClouderaManagerProduct product;

    public ClouderaManagerSingleParcelActivationListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, ClouderaManagerProduct product) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.product = product;
    }

    @Override
    protected String getClusterName(ClouderaManagerCommandPollerObject pollerObject) {
        return pollerObject.getStack().getName();
    }

    @Override
    protected String getProduct() {
        return product.getName();
    }

    @Override
    protected String getVersion() {
        return product.getVersion();
    }

    @Override
    protected Set<ParcelStatus> getExpectedParcelStatuses() {
        return ACTIVATED_STATUSES;
    }

    @Override
    protected String getCommandName() {
        return String.format("Activate parcel [%s]", product.getName());
    }
}
