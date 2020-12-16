package com.sequenceiq.mock.clouderamanager.base;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiRoleList;

@Controller
public class MgmtRolesResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    public ResponseEntity<ApiRoleList> createRoles(String mockUuid, @Valid ApiRoleList body) {
        return responseCreatorComponent.exec(new ApiRoleList().items(new ArrayList<>()));
    }

    public ResponseEntity<ApiRoleList> readRoles(String mockUuid) {
        return responseCreatorComponent.exec(new ApiRoleList().items(new ArrayList<>()));
    }

}
