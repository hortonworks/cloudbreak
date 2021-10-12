package com.sequenceiq.mock.clouderamanager.v31.controller;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.ParcelResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiParcel;
import com.sequenceiq.mock.swagger.v31.api.ParcelResourceApi;

@Controller
public class ParcelResourceV31Controller implements ParcelResourceApi {

    @Inject
    private ParcelResourceOperation parcelResourceOperation;

    @Override
    public ResponseEntity<ApiCommand> startDownloadCommand(String mockUuid, String clusterName, String product, String version) {
        return parcelResourceOperation.startDownloadCommand(mockUuid, clusterName, product, version);
    }

    @Override
    public ResponseEntity<ApiParcel> readParcel(String mockUuid, String clusterName, String product, String version) {
        return parcelResourceOperation.readParcel(mockUuid, clusterName, product, version);
    }

    @Override
    public ResponseEntity<ApiCommand> startDistributionCommand(String mockUuid, String clusterName, String product, String version) {
        return parcelResourceOperation.startDistributionCommand(mockUuid, clusterName, product, version);
    }

    @Override
    public ResponseEntity<ApiCommand> activateCommand(String mockUuid, String clusterName, String product, String version) {
        return parcelResourceOperation.activateCommand(mockUuid, clusterName, product, version);
    }
}
