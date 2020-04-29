package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerParcelsApiListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelsApiListenerTask.class);

    public ClouderaManagerParcelsApiListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject, CommandsResourceApi commandsResourceApi) {
        try {
            boolean parcelsAvailable = pollParcels(pollerObject);
            LOGGER.debug("Polling for parcel's availability returned: {}", parcelsAvailable);
            return parcelsAvailable;
        } catch (ApiException e) {
            LOGGER.debug("Cloudera Manager Parcels API is not available", e);
            return false;
        }
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to start Parcels API.");
    }

    private boolean pollParcels(ClouderaManagerPollerObject pollerObject) throws ApiException {
        ParcelsResourceApi parcelsResourceApi = new ParcelsResourceApi(pollerObject.getApiClient());
        ApiParcelList apiParcelList = parcelsResourceApi.readParcels(pollerObject.getStack().getName(), "");
        return Objects.nonNull(apiParcelList) && CollectionUtils.isNotEmpty(apiParcelList.getItems());
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject toolsResourceApi) {
        return "Parcels API is finally available";
    }

    @Override
    protected String getCommandName() {
        return "Wait for CM Parcels API to become available";
    }
}
