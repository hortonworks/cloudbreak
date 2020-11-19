package com.sequenceiq.mock.clouderamanager.v31.controller;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiRoleList;
import com.sequenceiq.mock.swagger.v31.api.MgmtRolesResourceApi;

@Controller
public class MgmtRolesResourceV31Controller implements MgmtRolesResourceApi {

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Override
    public ResponseEntity<ApiRoleList> createRoles(String mockUuid, @Valid ApiRoleList body) {
        return profileAwareComponent.exec(new ApiRoleList().items(new ArrayList<>()));
    }

    @Override
    public ResponseEntity<ApiRoleList> readRoles(String mockUuid) {
        return profileAwareComponent.exec(new ApiRoleList().items(new ArrayList<>()));
    }

}
