package com.sequenceiq.mock.clouderamanager.v31.controller;

import javax.inject.Inject;
import javax.validation.Valid;

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
    public ResponseEntity<ApiExternalUserMappingList> createExternalUserMappings(String mockUuid, @Valid ApiExternalUserMappingList body) {
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
    public ResponseEntity<ApiExternalUserMappingList> readExternalUserMappings(String mockUuid, @Valid String view) {
        return externalUserMappingsResourceOperation.readExternalUserMappings(mockUuid, view);
    }

    @Override
    public ResponseEntity<ApiExternalUserMapping> updateExternalUserMapping(String mockUuid, String uuid, @Valid ApiExternalUserMapping body) {
        return externalUserMappingsResourceOperation.updateExternalUserMapping(mockUuid, uuid, body);
    }
}
