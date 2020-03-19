package com.sequenceiq.cloudbreak.cm.polling.task;

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
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerUpgradeParcelDownloadListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerUpgradeParcelDownloadListenerTask.class);

    private ParcelResource parcelResource;

    public ClouderaManagerUpgradeParcelDownloadListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    public ClouderaManagerUpgradeParcelDownloadListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService, ParcelResource parcelResource) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
        this.parcelResource = parcelResource;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {

        ApiClient apiClient = pollerObject.getApiClient();
        ParcelResourceApi parcelResourceApi = clouderaManagerApiPojoFactory.getParcelResourceApi(apiClient);
        ApiParcel apiParcel = parcelResourceApi.readParcel(parcelResource.getClusterName(), parcelResource.getProduct(), parcelResource.getVersion());
        String parcelStage = apiParcel.getStage();

        if (!ParcelStatus.DOWNLOADED.name().equals(parcelStage)
                || !ParcelStatus.DISTRIBUTED.name().equals(parcelStage)
                || !ParcelStatus.ACTIVATED.name().equals(parcelStage)) {
            LOGGER.warn("Expected parcel status is {}, received status is: {}", ParcelStatus.DOWNLOADED.name(), parcelStage);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to download parcel.");
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject toolsResourceApi) {
        return "Successfully downloaded CDP Runtime parcel.";
    }

    @Override
    protected String getCommandName() {
        return "Download CDP Runtime parcel for upgrade.";
    }
}
