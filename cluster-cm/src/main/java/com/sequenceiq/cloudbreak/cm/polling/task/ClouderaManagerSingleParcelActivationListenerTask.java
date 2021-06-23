package com.sequenceiq.cloudbreak.cm.polling.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerSingleParcelActivationListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSingleParcelActivationListenerTask.class);

    private final ClouderaManagerProduct product;

    public ClouderaManagerSingleParcelActivationListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, ClouderaManagerProduct product) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
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
    protected String getCommandName() {
        return String.format("Activate parcel [%s]", product.getName());
    }
}
