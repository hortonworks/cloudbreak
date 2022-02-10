package com.sequenceiq.cloudbreak.cm.polling.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerUpgradeParcelDistributeListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerUpgradeParcelDistributeListenerTask.class);

    private ParcelResource parcelResource;

    public ClouderaManagerUpgradeParcelDistributeListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, ParcelResource parcelResource) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.parcelResource = parcelResource;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject) throws ApiException {
        ApiClient apiClient = pollerObject.getApiClient();
        ParcelResourceApi parcelResourceApi = clouderaManagerApiPojoFactory.getParcelResourceApi(apiClient);
        ApiParcel apiParcel = parcelResourceApi.readParcel(parcelResource.getClusterName(), parcelResource.getProduct(), parcelResource.getVersion());
        String parcelStage = apiParcel.getStage();

        if (!ParcelStatus.DISTRIBUTED.name().equals(parcelStage)
                && !ParcelStatus.ACTIVATED.name().equals(parcelStage)
                && !ParcelStatus.ACTIVATING.name().equals(parcelStage)) {
            LOGGER.warn("Expected parcel status is {}, received status is: {}", ParcelStatus.DISTRIBUTED.name(), parcelStage);
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected String getCommandName() {
        return "Distribute CDP Runtime parcel for upgrade.";
    }
}
