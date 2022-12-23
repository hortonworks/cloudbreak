package com.sequenceiq.cloudbreak.cm.polling.task;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelState;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public abstract class AbstractClouderaManagerParcelListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClouderaManagerParcelListenerTask.class);

    protected AbstractClouderaManagerParcelListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory, ClusterEventService clusterEventService) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
    }

    protected abstract String getClusterName(ClouderaManagerCommandPollerObject pollerObject);

    protected abstract String getProduct();

    protected abstract String getVersion();

    protected abstract Set<ParcelStatus> getExpectedParcelStatuses();

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject) throws ApiException {
        ApiParcel apiParcel = getApiParcel(pollerObject, getClusterName(pollerObject), getProduct(), getVersion());
        checkErrors(apiParcel);
        if (apiParcel.getStage() == null || !getExpectedParcelStatuses().contains(ParcelStatus.valueOf(apiParcel.getStage()))) {
            LOGGER.warn("Expected parcel status to be in {}, received status is: {}", getExpectedParcelStatuses(), apiParcel.getStage());
            return false;
        } else {
            return true;
        }
    }

    protected ApiParcel getApiParcel(ClouderaManagerCommandPollerObject pollerObject, String clusterName, String product, String version) throws ApiException {
        ApiClient apiClient = pollerObject.getApiClient();
        ParcelResourceApi parcelResourceApi = clouderaManagerApiPojoFactory.getParcelResourceApi(apiClient);
        return parcelResourceApi.readParcel(clusterName, product, version);
    }

    private void checkErrors(ApiParcel apiParcel) {
        List<String> errors = getIfNotNull(apiParcel.getState(), ApiParcelState::getErrors);
        if (!CollectionUtils.isEmpty(errors)) {
            throw new ClouderaManagerOperationFailedException(String.format("Command [%s] failed: %s", getPollingName(), String.join("; ", errors)));
        }
    }
}
