package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.ArrayList;
import java.util.List;
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
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerParcelActivationListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelActivationListenerTask.class);

    private final List<ClouderaManagerProduct> products;

    public ClouderaManagerParcelActivationListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, List<ClouderaManagerProduct> products) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.products = products;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        ApiClient apiClient = pollerObject.getApiClient();
        Stack stack = pollerObject.getStack();
        ApiParcelList parcels = getClouderaManagerParcels(apiClient, stack.getName());
        List<ApiParcel> notActivated = getNotActivatedOrMissingParcels(parcels);
        if (notActivated.isEmpty()) {
            LOGGER.debug("Parcels are activated.");
            return true;
        } else {
            LOGGER.debug("Some parcels are not yet activated: [{}].", getJoinedParcelStages(notActivated));
            return false;
        }
    }

    private ApiParcelList getClouderaManagerParcels(ApiClient apiClient, String stackName) throws ApiException {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiPojoFactory.getParcelsResourceApi(apiClient);
        return parcelsResourceApi.readParcels(stackName, "summary");
    }

    private List<ApiParcel> getNotActivatedOrMissingParcels(ApiParcelList parcels) {
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
        if (!ParcelStatus.ACTIVATED.name().equals(matchingParcel.getStage())) {
            notActivated.add(matchingParcel);
        }
    }

    private String getJoinedParcelStages(List<ApiParcel> notActivated) {
        return notActivated.stream()
                .map(parcel -> String.format("(%s %s : %s)", parcel.getProduct(), parcel.getVersion(), parcel.getStage()))
                .collect(Collectors.joining(", "));
    }

    @Override
    protected String getCommandName() {
        return "Parcel activation";
    }
}
