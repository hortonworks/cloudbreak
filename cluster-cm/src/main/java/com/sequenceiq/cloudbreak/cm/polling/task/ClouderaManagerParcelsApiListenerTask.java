package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerParcelsApiListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelsApiListenerTask.class);

    public ClouderaManagerParcelsApiListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) {
        try {
            boolean parcelsAvailable = pollParcels(pollerObject);
            LOGGER.debug("Polling for parcel's availability returned: {}", parcelsAvailable);
            return parcelsAvailable;
        } catch (ApiException e) {
            LOGGER.debug("Cloudera Manager Parcels API is not available", e);
            return false;
        }
    }

    private boolean pollParcels(ClouderaManagerCommandPollerObject pollerObject) throws ApiException {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiPojoFactory.getParcelsResourceApi(pollerObject.getApiClient());
        ApiParcelList apiParcelList = parcelsResourceApi.readParcels(pollerObject.getStack().getName(), "");
        return Objects.nonNull(apiParcelList) && CollectionUtils.isNotEmpty(apiParcelList.getItems());
    }

    @Override
    protected String getCommandName() {
        return "Wait for CM Parcels API to become available";
    }
}
