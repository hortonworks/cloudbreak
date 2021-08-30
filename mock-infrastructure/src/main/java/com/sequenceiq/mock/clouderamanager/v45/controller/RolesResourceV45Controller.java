package com.sequenceiq.mock.clouderamanager.v45.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.RolesResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiRole;
import com.sequenceiq.mock.swagger.model.ApiRoleList;
import com.sequenceiq.mock.swagger.v45.api.RolesResourceApi;

@Controller
public class RolesResourceV45Controller implements RolesResourceApi {

    @Inject
    private RolesResourceOperation rolesResourceOperation;

    @Override
    public ResponseEntity<ApiRoleList> createRoles(String mockUuid, String clusterName, String serviceName, @Valid ApiRoleList body) {
        return rolesResourceOperation.createRoles(mockUuid, clusterName, serviceName, body);
    }

    @Override
    public ResponseEntity<ApiRole> deleteRole(String mockUuid, String clusterName, String roleName, String serviceName) {
        return rolesResourceOperation.deleteRole(mockUuid, clusterName, roleName, serviceName);
    }

    @Override
    public ResponseEntity<ApiRoleList> readRoles(String mockUuid, String clusterName, String serviceName, @Valid String filter, @Valid String view) {
        return rolesResourceOperation.readRoles(mockUuid, clusterName, serviceName, filter, view);
    }
}
