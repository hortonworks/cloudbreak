package com.sequenceiq.cloudbreak.cm.polling.task;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Service
public class ClouderaManagerDeployClientConfigListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDeployClientConfigListenerTask.class);

    private static final String PARCEL_ACTIVATED_STAGE = "ACTIVATED";

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Override
    public boolean checkStatus(ClouderaManagerCommandPollerObject clouderaManagerPollerObject) {
        ApiClient apiClient = clouderaManagerPollerObject.getApiClient();

        ParcelResourceApi parcelResourceApi = clouderaManagerClientFactory.getParcelResourceApi(apiClient);
        Stack stack = clouderaManagerPollerObject.getStack();
        ClouderaManagerProduct clouderaManagerProduct = Optional.ofNullable(stack).map(Stack::getCluster).map(Cluster::getComponents)
                .orElse(Set.of())
                .stream()
                .filter(clusterComponent -> ComponentType.CDH_PRODUCT_DETAILS.equals(clusterComponent.getComponentType()))
                .map(ClusterComponent::getAttributes)
                .map(toAttributeClass(ClouderaManagerProduct.class))
                .findFirst()
                .orElseThrow(() -> new CloudbreakServiceException("Failed to get CDH component details."));

        try {
            ApiParcel parcel = parcelResourceApi.readParcel(stack.getName(), clouderaManagerProduct.getName(), clouderaManagerProduct.getVersion());
            if (PARCEL_ACTIVATED_STAGE.equals(parcel.getStage())) {
                LOGGER.debug("Parcel activated");
                return true;
            } else {
                LOGGER.debug("Parcel not yet activated. Stage: [{}]", parcel.getStage());
                return false;
            }
        } catch (ApiException e) {
            LOGGER.debug("Cloudera Manager is not running", e);
            return false;
        }
    }

    private <T> Function<Json, T> toAttributeClass(Class<T> attributeClass) {
        return attribute -> {
            try {
                return Optional.ofNullable(attribute)
                        .orElseThrow(() -> new CloudbreakServiceException("Cluster component attribute json cannot be null."))
                        .get(attributeClass);
            } catch (IOException e) {
                throw new CloudbreakServiceException("Cannot deserialize the compnent: " + attributeClass, e);
            }
        };
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to deploy client configurations.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return "Cloudera Manager deployed client configurations finished with success result.";
    }

    @Override
    protected String getCommandName() {
        return "Deploy client configurations";
    }
}
