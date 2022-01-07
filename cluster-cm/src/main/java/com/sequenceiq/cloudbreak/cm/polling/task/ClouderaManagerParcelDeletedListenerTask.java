package com.sequenceiq.cloudbreak.cm.polling.task;

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
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerParcelDeletedListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelDeletedListenerTask.class);

    private final Multimap<String, String> parcelVersions;

    public ClouderaManagerParcelDeletedListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, Multimap<String, String> parcelVersions) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.parcelVersions = parcelVersions;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        ApiClient apiClient = pollerObject.getApiClient();
        Stack stack = pollerObject.getStack();
        ApiParcelList parcels = getClouderaManagerParcels(apiClient, stack.getName());
        List<ApiParcel> existedParcels = collectExistingParcels(parcels);
        if (existedParcels.isEmpty()) {
            LOGGER.debug("Parcels are deleted successfully.");
            return true;
        } else {
            LOGGER.debug("Some parcels are not yet deleted: [{}].", getJoinedParcelStages(existedParcels));
            return false;
        }
    }

    private ApiParcelList getClouderaManagerParcels(ApiClient apiClient, String stackName) throws ApiException {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiPojoFactory.getParcelsResourceApi(apiClient);
        return parcelsResourceApi.readParcels(stackName, "summary");
    }

    private List<ApiParcel> collectExistingParcels(ApiParcelList parcelsFromCM) {
        return parcelsFromCM.getItems().stream()
                .filter(this::isMatchingParcel)
                .filter(parcel -> !ParcelStatus.AVAILABLE_REMOTELY.name().equals(parcel.getStage()))
                .collect(Collectors.toList());
    }

    private boolean isMatchingParcel(ApiParcel parcel) {
        return parcelVersions.containsEntry(parcel.getProduct(), parcel.getVersion());
    }

    private String getJoinedParcelStages(List<ApiParcel> notActivated) {
        return notActivated.stream()
                .map(parcel -> String.format("(%s %s : %s)", parcel.getProduct(), parcel.getVersion(), parcel.getStage()))
                .collect(Collectors.joining(", "));
    }

    @Override
    protected String getCommandName() {
        return String.format("Parcel deletion checker task of %s parcels", parcelVersions);
    }
}
