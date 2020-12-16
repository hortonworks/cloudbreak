package com.sequenceiq.mock.clouderamanager.v31.controller;

import javax.inject.Inject;
import javax.validation.Valid;

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
    public ResponseEntity<ApiRoleList> createRoles(String mockUuid, @Valid ApiRoleList body) {
        return mgmtRolesResourceOperation.createRoles(mockUuid, body);
    }

    @Override
    public ResponseEntity<ApiRoleList> readRoles(String mockUuid) {
        return mgmtRolesResourceOperation.readRoles(mockUuid);
    }

}
