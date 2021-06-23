package com.sequenceiq.cloudbreak.cm.polling.task;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelState;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerUpgradeParcelDownloadListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerUpgradeParcelDownloadListenerTask.class);

    private ParcelResource parcelResource;

    public ClouderaManagerUpgradeParcelDownloadListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
    }

    public ClouderaManagerUpgradeParcelDownloadListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, ParcelResource parcelResource) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.parcelResource = parcelResource;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {

        ApiParcel apiParcel = getApiParcel(pollerObject);
        String parcelStage = apiParcel.getStage();

        if (!ParcelStatus.DOWNLOADED.name().equals(parcelStage)
                && !ParcelStatus.DISTRIBUTED.name().equals(parcelStage)
                && !ParcelStatus.DISTRIBUTING.name().equals(parcelStage)
                && !ParcelStatus.ACTIVATED.name().equals(parcelStage)
                && !ParcelStatus.ACTIVATING.name().equals(parcelStage)) {
            LOGGER.warn("Expected parcel status is {}, received status is: {}", ParcelStatus.DOWNLOADED.name(), parcelStage);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        //when downloading, progress and totalProgress will show the current number of bytes downloaded
        // and the total number of bytes needed to be downloaded respectively.
        String baseMessage = "Operation timed out. Failed to download parcel in time.";
        try {
            ApiParcel apiParcel = getApiParcel(pollerObject);
            ApiParcelState parcelState = apiParcel.getState();
            String progress = FileUtils.byteCountToDisplaySize(parcelState.getProgress().toBigInteger());
            String totalProgress = FileUtils.byteCountToDisplaySize(parcelState.getTotalProgress().toBigInteger());
            String progressMessage = String.format(" %s out of total %s has been downloaded!", progress, totalProgress);
            throw new ClouderaManagerOperationFailedException(baseMessage + progressMessage);
        } catch (ApiException e) {
            throw new ClouderaManagerOperationFailedException(baseMessage);
        }
    }

    private ApiParcel getApiParcel(ClouderaManagerCommandPollerObject pollerObject) throws ApiException {
        ApiClient apiClient = pollerObject.getApiClient();
        ParcelResourceApi parcelResourceApi = clouderaManagerApiPojoFactory.getParcelResourceApi(apiClient);
        return parcelResourceApi.readParcel(parcelResource.getClusterName(), parcelResource.getProduct(), parcelResource.getVersion());
    }

    @Override
    protected String getCommandName() {
        return "Download CDP Runtime parcel for upgrade.";
    }
}
