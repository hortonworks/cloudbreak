package com.sequenceiq.mock.clouderamanager.v31.controller;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiRole;
import com.sequenceiq.mock.swagger.model.ApiRoleList;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import com.sequenceiq.mock.swagger.v31.api.RolesResourceApi;

@Controller
public class RolesResourceV31Controller implements RolesResourceApi {

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Override
    public ResponseEntity<ApiRoleList> createRoles(String mockUuid, String clusterName, String serviceName, @Valid ApiRoleList body) {
        ApiRoleList roleList = new ApiRoleList().items(List.of(new ApiRole().name("role1").serviceRef(new ApiServiceRef().serviceName("service1"))));
        return profileAwareComponent.exec(roleList);
    }

    @Override
    public ResponseEntity<ApiRole> deleteRole(String mockUuid, String clusterName, String roleName, String serviceName) {
        return profileAwareComponent.exec(new ApiRole().name("role1"));
    }

    @Override
    public ResponseEntity<ApiRoleList> readRoles(String mockUuid, String clusterName, String serviceName, @Valid String filter, @Valid String view) {
        ApiRoleList roleList = new ApiRoleList().items(List.of(new ApiRole().name("role1").serviceRef(new ApiServiceRef().serviceName(serviceName))));
        return profileAwareComponent.exec(roleList);
    }
}
