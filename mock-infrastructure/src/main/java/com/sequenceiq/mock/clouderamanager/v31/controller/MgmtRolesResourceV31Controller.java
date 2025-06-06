package com.sequenceiq.mock.clouderamanager.v31.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.MgmtRolesResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiRoleList;
import com.sequenceiq.mock.swagger.v31.api.MgmtRolesResourceApi;

@Controller
public class MgmtRolesResourceV31Controller implements MgmtRolesResourceApi {

    @Inject
    private MgmtRolesResourceOperation mgmtRolesResourceOperation;

    @Override
    public ResponseEntity<ApiRoleList> createRoles(String mockUuid, ApiRoleList body) {
        return mgmtRolesResourceOperation.createRoles(mockUuid, body);
    }

    @Override
    public ResponseEntity<ApiRoleList> readRoles(String mockUuid) {
        return mgmtRolesResourceOperation.readRoles(mockUuid);
    }

}
