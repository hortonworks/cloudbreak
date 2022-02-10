package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class ClouderaManagerParcelStatusListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelStatusListenerTask.class);

    private final Multimap<String, String> parcelVersions;

    private final ParcelStatus parcelStatus;

    public ClouderaManagerParcelStatusListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, Multimap<String, String> parcelVersions, ParcelStatus parcelStatus) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.parcelVersions = parcelVersions;
        this.parcelStatus = parcelStatus;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject) throws ApiException {
        ApiClient apiClient = pollerObject.getApiClient();
        Stack stack = pollerObject.getStack();
        ApiParcelList parcels = getClouderaManagerParcels(apiClient, stack.getName());
        List<ApiParcel> notInProperStateParcels = getNotInProperStateParcels(parcels);
        if (notInProperStateParcels.isEmpty()) {
            LOGGER.debug("Parcels are in the proper statuses: {}", parcelStatus);
            return true;
        } else {
            LOGGER.debug("Some parcels are not yet in the proper status: [{}].", getJoinedParcelStages(notInProperStateParcels));
            return false;
        }
    }

    private ApiParcelList getClouderaManagerParcels(ApiClient apiClient, String stackName) throws ApiException {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiPojoFactory.getParcelsResourceApi(apiClient);
        return parcelsResourceApi.readParcels(stackName, "summary");
    }

    private List<ApiParcel> getNotInProperStateParcels(ApiParcelList parcels) {
        return parcels.getItems().stream()
                .filter(parcel -> parcelVersions.containsEntry(parcel.getProduct(), parcel.getVersion()))
                .filter(parcel -> !parcelStatus.name().equals(parcel.getStage()))
                .collect(Collectors.toList());
    }

    private String getJoinedParcelStages(List<ApiParcel> notActivated) {
        return notActivated.stream()
                .map(parcel -> String.format("(%s %s : %s)", parcel.getProduct(), parcel.getVersion(), parcel.getStage()))
                .collect(Collectors.joining(", "));
    }

    @Override
    protected String getCommandName() {
        return String.format("Parcel status checker task of %s parcels for [%s] status", parcelVersions, parcelStatus);
    }
}
