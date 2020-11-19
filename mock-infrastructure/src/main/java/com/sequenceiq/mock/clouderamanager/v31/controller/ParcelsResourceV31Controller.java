package com.sequenceiq.mock.clouderamanager.v31.controller;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiParcel;
import com.sequenceiq.mock.swagger.model.ApiParcelList;
import com.sequenceiq.mock.swagger.model.ApiProductVersion;
import com.sequenceiq.mock.swagger.v31.api.ParcelsResourceApi;

@Controller
public class ParcelsResourceV31Controller implements ParcelsResourceApi {

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Override
    public ResponseEntity<ApiParcelList> readParcels(String mockUuid, String clusterName, @Valid String view) {
        List<ApiProductVersion> products = clouderaManagerStoreService.getClouderaManagerProducts(mockUuid);

        ApiParcelList items = new ApiParcelList().items(
                products.stream().map(product -> new ApiParcel()
                        .product(product.getProduct())
                        .version(product.getVersion())
                        .stage("ACTIVATED"))
                        .collect(Collectors.toList())
        );
        return profileAwareComponent.exec(items);
    }
}
