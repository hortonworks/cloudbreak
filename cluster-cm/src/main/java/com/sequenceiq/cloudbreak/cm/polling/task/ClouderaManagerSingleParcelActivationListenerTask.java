package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerSingleParcelActivationListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSingleParcelActivationListenerTask.class);

    private final ClouderaManagerProduct product;

    public ClouderaManagerSingleParcelActivationListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService, ClouderaManagerProduct product) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
        this.product = product;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        ApiClient apiClient = pollerObject.getApiClient();
        ParcelResourceApi parcelResourceApi = clouderaManagerApiPojoFactory.getParcelResourceApi(apiClient);
        String parcelStatus = getParcelStatus(pollerObject, parcelResourceApi);
        if (ParcelStatus.ACTIVATED.name().equals(parcelStatus)) {
            LOGGER.debug("{} parcel is activated.", product.getName());
            return true;
        } else {
            LOGGER.debug("{} [{}] parcel is not yet activated. Current status: {}.", product.getName(), product.getVersion(), parcelStatus);
            return false;
        }
    }

    private String getParcelStatus(ClouderaManagerCommandPollerObject pollerObject, ParcelResourceApi parcelResourceApi) throws ApiException {
        return parcelResourceApi.readParcel(pollerObject.getStack().getName(), product.getName(), product.getVersion()).getStage();
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        getCloudbreakEventService().fireClusterManagerEvent(pollerObject.getStack().getId(), pollerObject.getStack().getStatus().name(),
                ResourceEvent.CLUSTER_CM_COMMAND_TIMEOUT, Optional.of(pollerObject.getId()));
        throw new ClouderaManagerOperationFailedException(String.format("Operation timed out. Failed to activate %s parcel.", product.getName()));
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return String.format("%s parcel activation finished with success result.", product.getName());
    }

    @Override
    protected String getCommandName() {
        return "Activate parcel";
    }
}
