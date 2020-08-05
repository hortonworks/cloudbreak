package com.sequenceiq.cloudbreak.cm;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;

@Service
class ClouderaManagerParcelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelService.class);

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public Map<String, String> getActivatedParcels(ApiClient client, String stackName) throws ApiException {
        return getClouderaManagerParcelsByStatus(client, stackName, ParcelStatus.ACTIVATED)
                .stream()
                .collect(Collectors.toMap(ApiParcel::getProduct, ApiParcel::getVersion));
    }

    private List<ApiParcel> getClouderaManagerParcelsByStatus(ApiClient client, String stackName, ParcelStatus parcelStatus) throws ApiException {
        ApiParcelList parcelList = getClouderaManagerParcels(client, stackName);
        return parcelList.getItems()
                .stream()
                .filter(parcel -> parcelStatus.name().equals(parcel.getStage()))
                .peek(parcel -> LOGGER.debug("Parcel {} is found with status {}", parcel.getDisplayName(), parcelStatus))
                .collect(toList());
    }

    private ApiParcelList getClouderaManagerParcels(ApiClient client, String stackName) throws ApiException {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(client);
        return parcelsResourceApi.readParcels(stackName, "summary");
    }
}