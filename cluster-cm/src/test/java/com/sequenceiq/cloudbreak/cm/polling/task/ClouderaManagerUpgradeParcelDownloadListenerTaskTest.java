package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.mockito.Mockito.when;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;

class ClouderaManagerUpgradeParcelDownloadListenerTaskTest
        extends AbstractClouderaManagerParcelListenerTaskTest<ClouderaManagerUpgradeParcelDownloadListenerTask> {

    @Override
    ClouderaManagerUpgradeParcelDownloadListenerTask setUpUnderTest() {
        ParcelResource parcelResource = new ParcelResource(CLUSTER_NAME, PRODUCT, VERSION);
        return new ClouderaManagerUpgradeParcelDownloadListenerTask(clouderaManagerApiPojoFactory, clusterEventService, parcelResource);
    }

    @Test
    void handleTimeoutSuccess() {
        when(apiParcelState.getProgress()).thenReturn(10L);
        when(apiParcelState.getTotalProgress()).thenReturn(100L);

        Assertions.assertThatThrownBy(() -> underTest.handleTimeout(pollerObject))
                .isInstanceOf(ClouderaManagerOperationFailedException.class)
                .hasMessage("Operation timed out. Failed to download parcel in time. 10 bytes out of total 100 bytes has been downloaded!");
    }

    @Test
    void handleTimeoutFailure() throws ApiException {
        when(parcelResourceApi.readParcel(CLUSTER_NAME, PRODUCT, VERSION)).thenThrow(new ApiException());

        Assertions.assertThatThrownBy(() -> underTest.handleTimeout(pollerObject))
                .isInstanceOf(ClouderaManagerOperationFailedException.class)
                .hasMessage("Operation timed out. Failed to download parcel in time.");
    }
}
