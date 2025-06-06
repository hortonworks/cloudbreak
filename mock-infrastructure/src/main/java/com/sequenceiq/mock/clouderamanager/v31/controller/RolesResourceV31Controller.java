package com.sequenceiq.mock.clouderamanager.v31.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.RolesResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiRole;
import com.sequenceiq.mock.swagger.model.ApiRoleList;
import com.sequenceiq.mock.swagger.v31.api.RolesResourceApi;

@Controller
public class RolesResourceV31Controller implements RolesResourceApi {

    @Inject
    private RolesResourceOperation rolesResourceOperation;

    @Override
    public ResponseEntity<ApiRoleList> createRoles(String mockUuid, String clusterName, String serviceName, ApiRoleList body) {
        return rolesResourceOperation.createRoles(mockUuid, clusterName, serviceName, body);
    }

    @Override
    public ResponseEntity<ApiRole> deleteRole(String mockUuid, String clusterName, String roleName, String serviceName) {
        return rolesResourceOperation.deleteRole(mockUuid, clusterName, roleName, serviceName);
    }

    @Override
    public ResponseEntity<ApiRoleList> readRoles(String mockUuid, String clusterName, String serviceName, String filter, String view) {
        return rolesResourceOperation.readRoles(mockUuid, clusterName, serviceName, filter, view);
    }
}
