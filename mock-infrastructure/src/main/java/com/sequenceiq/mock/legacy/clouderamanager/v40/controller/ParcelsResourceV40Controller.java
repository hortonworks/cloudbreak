package com.sequenceiq.mock.legacy.clouderamanager.v40.controller;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DataProviderService;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.swagger.v40.api.ParcelsResourceApi;
import com.sequenceiq.mock.swagger.model.ApiParcel;
import com.sequenceiq.mock.swagger.model.ApiParcelList;
import com.sequenceiq.mock.swagger.model.ApiParcelUsage;
import com.sequenceiq.mock.swagger.model.ApiProductVersion;

@Controller
public class ParcelsResourceV40Controller implements ParcelsResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<ApiParcelUsage> getParcelUsage(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiParcelList> readParcels(String clusterName, @Valid String view) {
        List<ApiProductVersion> products = defaultModelService.getClouderaManagerProducts();

        ApiParcelList items = new ApiParcelList().items(
                products.stream().map(product -> new ApiParcel()
                        .product(product.getProduct())
                        .version(product.getVersion())
                        .stage("ACTIVATED"))
                        .collect(Collectors.toList())
        );
        return ProfileAwareResponse.exec(items, defaultModelService);
    }
}
