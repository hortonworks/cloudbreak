package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelState;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerUpgradeParcelDownloadListenerTask extends AbstractClouderaManagerParcelListenerTask {

    private static final Set<ParcelStatus> DOWNLOADED_STATUSES = Set.of(
            ParcelStatus.DOWNLOADED,
            ParcelStatus.DISTRIBUTED,
            ParcelStatus.DISTRIBUTING,
            ParcelStatus.ACTIVATED,
            ParcelStatus.ACTIVATING
    );

    private final ParcelResource parcelResource;

    public ClouderaManagerUpgradeParcelDownloadListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
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
        return DOWNLOADED_STATUSES;
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        //when downloading, progress and totalProgress will show the current number of bytes downloaded
        // and the total number of bytes needed to be downloaded respectively.
        String baseMessage = "Operation timed out. Failed to download parcel in time.";
        try {
            ApiParcel apiParcel = getApiParcel(pollerObject, parcelResource.getClusterName(), parcelResource.getProduct(), parcelResource.getVersion());
            ApiParcelState parcelState = apiParcel.getState();
            String progress = FileUtils.byteCountToDisplaySize(parcelState.getProgress().toBigInteger());
            String totalProgress = FileUtils.byteCountToDisplaySize(parcelState.getTotalProgress().toBigInteger());
            String progressMessage = String.format(" %s out of total %s has been downloaded!", progress, totalProgress);
            throw new ClouderaManagerOperationFailedException(baseMessage + progressMessage);
        } catch (ApiException e) {
            throw new ClouderaManagerOperationFailedException(baseMessage);
        }
    }

    @Override
    protected String getCommandName() {
        return "Download CDP Runtime parcel for upgrade.";
    }
}
