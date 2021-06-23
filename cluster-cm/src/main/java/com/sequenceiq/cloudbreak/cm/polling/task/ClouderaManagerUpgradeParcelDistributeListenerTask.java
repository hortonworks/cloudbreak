package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerUpgradeParcelDistributeListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerUpgradeParcelDistributeListenerTask.class);

    private ParcelResource parcelResource;

    public ClouderaManagerUpgradeParcelDistributeListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    public ClouderaManagerUpgradeParcelDistributeListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService, ParcelResource parcelResource) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
        this.parcelResource = parcelResource;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {

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
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        getCloudbreakEventService().fireClusterManagerEvent(pollerObject.getStack().getId(), pollerObject.getStack().getStatus().name(),
                ResourceEvent.CLUSTER_CM_COMMAND_TIMEOUT, Optional.of(pollerObject.getId()));
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to distribute parcel in time.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return "Successfully distributed CDP Runtime parcel.";
    }

    @Override
    protected String getCommandName() {
        return "Distribute CDP Runtime parcel for upgrade.";
    }
}
