package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;

public class ClouderaManagerParcelsApiListenerTask extends AbstractClouderaManagerApiCheckerTask<ClouderaManagerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelsApiListenerTask.class);

    public ClouderaManagerParcelsApiListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject) {
        try {
            boolean parcelsAvailable = pollParcels(pollerObject);
            LOGGER.debug("Polling for parcel's availability returned: {}", parcelsAvailable);
            return parcelsAvailable;
        } catch (ApiException e) {
            LOGGER.debug("Cloudera Manager Parcels API is not available", e);
            return false;
        }
    }

    private boolean pollParcels(ClouderaManagerPollerObject pollerObject) throws ApiException {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiPojoFactory.getParcelsResourceApi(pollerObject.getApiClient());
        ApiParcelList apiParcelList = parcelsResourceApi.readParcels(pollerObject.getStack().getName(), "");
        return Objects.nonNull(apiParcelList) && CollectionUtils.isNotEmpty(apiParcelList.getItems());
    }

    @Override
    protected String getPollingName() {
        return "Wait for CM Parcels API to become available";
    }
}
