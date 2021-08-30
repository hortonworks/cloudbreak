package com.sequenceiq.mock.clouderamanager.v45.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.ParcelsResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiParcelList;
import com.sequenceiq.mock.swagger.v45.api.ParcelsResourceApi;

@Controller
public class ParcelsResourceV45Controller implements ParcelsResourceApi {

    @Inject
    private ParcelsResourceOperation parcelsResourceOperation;

    @Override
    public ResponseEntity<ApiParcelList> readParcels(String mockUuid, String clusterName, @Valid String view) {
        return parcelsResourceOperation.readParcels(mockUuid, clusterName, view);
    }
}
