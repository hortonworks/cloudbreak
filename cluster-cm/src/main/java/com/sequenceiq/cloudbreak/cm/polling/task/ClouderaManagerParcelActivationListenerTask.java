package com.sequenceiq.cloudbreak.cm.polling.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

public class ClouderaManagerParcelActivationListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelActivationListenerTask.class);

    private static final String PARCEL_ACTIVATED_STAGE = "ACTIVATED";

    public ClouderaManagerParcelActivationListenerTask(ClouderaManagerClientFactory clouderaManagerClientFactory) {
        super(clouderaManagerClientFactory);
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        ApiClient apiClient = pollerObject.getApiClient();
        Stack stack = pollerObject.getStack();
        List<ClouderaManagerProduct> clouderaManagerProducts = getClouderaManagerProductsFromStack(stack);
        ApiParcelList parcels = getClouderaManagerParcels(apiClient, stack.getName());
        List<ApiParcel> notActivated = getNotActivatedOrMissingParcels(clouderaManagerProducts, parcels);
        if (notActivated.isEmpty()) {
            LOGGER.debug("Parcels are activated.");
            return true;
        } else {
            LOGGER.debug("Some parcels are not yet activated: [{}].", getJoinedParcelStages(notActivated));
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
                throw new CloudbreakServiceException("Cannot deserialize the component: " + attributeClass, e);
            }
        };
    }

    private List<ClouderaManagerProduct> getClouderaManagerProductsFromStack(Stack stack) {
        return Optional.ofNullable(stack).map(Stack::getCluster).map(Cluster::getComponents)
                .orElse(Set.of())
                .stream()
                .filter(clusterComponent -> ComponentType.CDH_PRODUCT_DETAILS.equals(clusterComponent.getComponentType()))
                .map(ClusterComponent::getAttributes)
                .map(toAttributeClass(ClouderaManagerProduct.class))
                .collect(Collectors.toList());
    }

    private ApiParcelList getClouderaManagerParcels(ApiClient apiClient, String stackName) throws ApiException {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerClientFactory.getParcelsResourceApi(apiClient);
        return parcelsResourceApi.readParcels(stackName, "summary");
    }

    private List<ApiParcel> getNotActivatedOrMissingParcels(List<ClouderaManagerProduct> products, ApiParcelList parcels) {
        List<ApiParcel> notActivated = new ArrayList<>(parcels.getItems().size());
        products.forEach(product -> findParcelAndAddNotActivated(product, parcels, notActivated));
        return notActivated;
    }

    private void findParcelAndAddNotActivated(ClouderaManagerProduct product, ApiParcelList parcels, List<ApiParcel> notActivated) {
        parcels.getItems()
                .stream()
                .filter(parcel -> isProductMatching(product, parcel))
                .findFirst()
                .ifPresentOrElse(matchingParcel -> addNotActivated(matchingParcel, notActivated),
                        () -> notActivated.add(new ApiParcel().stage("MISSING").product(product.getName()).version(product.getVersion())));
    }

    private boolean isProductMatching(ClouderaManagerProduct product, ApiParcel parcel) {
        return product.getName().equals(parcel.getProduct())
                && product.getVersion().equals(parcel.getVersion());
    }

    private void addNotActivated(ApiParcel matchingParcel, List<ApiParcel> notActivated) {
        if (!PARCEL_ACTIVATED_STAGE.equals(matchingParcel.getStage())) {
            notActivated.add(matchingParcel);
        }
    }

    private String getJoinedParcelStages(List<ApiParcel> notActivated) {
        return notActivated.stream()
                .map(parcel -> String.format("(%s %s : %s)", parcel.getProduct(), parcel.getVersion(), parcel.getStage()))
                .collect(Collectors.joining(", "));
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to deploy client configurations.");
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject toolsResourceApi) {
        return "Cloudera Manager deployed client configurations finished with success result.";
    }

    @Override
    protected String getCommandName() {
        return "Deploy client configurations";
    }
}
