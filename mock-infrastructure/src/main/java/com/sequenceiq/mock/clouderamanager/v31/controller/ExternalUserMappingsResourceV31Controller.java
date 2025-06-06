package com.sequenceiq.mock.clouderamanager.v31.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.ExternalUserMappingsResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiExternalUserMapping;
import com.sequenceiq.mock.swagger.model.ApiExternalUserMappingList;
import com.sequenceiq.mock.swagger.v31.api.ExternalUserMappingsResourceApi;

@Controller
public class ExternalUserMappingsResourceV31Controller implements ExternalUserMappingsResourceApi {

    @Inject
    private ExternalUserMappingsResourceOperation externalUserMappingsResourceOperation;

    @Override
    public ResponseEntity<ApiExternalUserMappingList> createExternalUserMappings(String mockUuid, ApiExternalUserMappingList body) {
        return externalUserMappingsResourceOperation.createExternalUserMappings(mockUuid, body);
    }

    @Override
    public ResponseEntity<ApiExternalUserMapping> deleteExternalUserMapping(String mockUuid, String uuid) {
        return externalUserMappingsResourceOperation.deleteExternalUserMapping(mockUuid, uuid);
    }

    @Override
    public ResponseEntity<ApiExternalUserMapping> readExternalUserMapping(String mockUuid, String uuid) {
        return externalUserMappingsResourceOperation.readExternalUserMapping(mockUuid, uuid);
    }

    @Override
    public ResponseEntity<ApiExternalUserMappingList> readExternalUserMappings(String mockUuid, String view) {
        return externalUserMappingsResourceOperation.readExternalUserMappings(mockUuid, view);
    }

    @Override
    public ResponseEntity<ApiExternalUserMapping> updateExternalUserMapping(String mockUuid, String uuid, ApiExternalUserMapping body) {
        return externalUserMappingsResourceOperation.updateExternalUserMapping(mockUuid, uuid, body);
    }
}
