package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerParcelStatusListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelStatusListenerTask.class);

    private Map<String, String> parcelVersions;

    private ParcelStatus parcelStatus;

    public ClouderaManagerParcelStatusListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService, Map<String, String> parcelVersions, ParcelStatus parcelStatus) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
        this.parcelVersions = parcelVersions;
        this.parcelStatus = parcelStatus;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
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
                .filter(parcel -> parcelVersions.containsKey(parcel.getProduct()))
                .filter(parcel -> parcelVersions.get(parcel.getProduct()).equals(parcel.getVersion()))
                .filter(parcel -> !parcelStatus.name().equals(parcel.getStage()))
                .collect(Collectors.toList());
    }

    private String getJoinedParcelStages(List<ApiParcel> notActivated) {
        return notActivated.stream()
                .map(parcel -> String.format("(%s %s : %s)", parcel.getProduct(), parcel.getVersion(), parcel.getStage()))
                .collect(Collectors.joining(", "));
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        getCloudbreakEventService().fireClusterManagerEvent(pollerObject.getStack().getId(), pollerObject.getStack().getStatus().name(),
                ResourceEvent.CLUSTER_CM_COMMAND_TIMEOUT, Optional.of(pollerObject.getId()));
        throw new ClouderaManagerOperationFailedException("Operation timed out. Parcels are not in the proper state.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return String.format("Cloudera Manager parcels %s are in the proper state [%s]", parcelVersions, parcelStatus);
    }

    @Override
    protected String getCommandName() {
        return String.format("Parcel status checker task of %s parcels for [%s] status", parcelVersions, parcelStatus);
    }
}
