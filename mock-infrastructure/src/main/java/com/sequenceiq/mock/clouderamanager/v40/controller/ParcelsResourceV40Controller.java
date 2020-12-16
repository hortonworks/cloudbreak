package com.sequenceiq.mock.clouderamanager.v40.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.ParcelsResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiParcelList;
import com.sequenceiq.mock.swagger.v40.api.ParcelsResourceApi;

@Controller
public class ParcelsResourceV40Controller implements ParcelsResourceApi {

    @Inject
    private ParcelsResourceOperation parcelsResourceOperation;

    @Override
    public ResponseEntity<ApiParcelList> readParcels(String mockUuid, String clusterName, @Valid String view) {
        return parcelsResourceOperation.readParcels(mockUuid, clusterName, view);
    }
}
