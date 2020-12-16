package com.sequenceiq.mock.clouderamanager.base;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiParcel;
import com.sequenceiq.mock.swagger.model.ApiParcelList;
import com.sequenceiq.mock.swagger.model.ApiProductVersion;

@Controller
public class ParcelsResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    public ResponseEntity<ApiParcelList> readParcels(String mockUuid, String clusterName, @Valid String view) {
        List<ApiProductVersion> products = clouderaManagerStoreService.getClouderaManagerProducts(mockUuid);
        ApiParcelList apiParcelList = new ApiParcelList();
        if (!CollectionUtils.isEmpty(products)) {
            apiParcelList.items(
                    products.stream().map(product -> new ApiParcel()
                            .product(product.getProduct())
                            .version(product.getVersion())
                            .stage("ACTIVATED"))
                            .collect(Collectors.toList())
            );
        } else {
            apiParcelList.setItems(Collections.emptyList());
        }
        return responseCreatorComponent.exec(apiParcelList);
    }
}
