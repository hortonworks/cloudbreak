package com.sequenceiq.mock.legacy.clouderamanager.v31.controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.swagger.v31.api.AuthRolesResourceApi;
import com.sequenceiq.mock.swagger.model.ApiAuthRole;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleList;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleMetadataList;

@Controller
public class AuthRolesResourceV31Controller implements AuthRolesResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Override
    public ResponseEntity<ApiAuthRoleList> createAuthRoles(@Valid ApiAuthRoleList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiAuthRole> deleteAuthRole(String uuid) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiAuthRole> readAuthRole(String uuid) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiAuthRoleList> readAuthRoles(@Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiAuthRoleMetadataList> readAuthRolesMetadata(@Valid String view) {
        return ProfileAwareResponse.exec(new ApiAuthRoleMetadataList(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiAuthRole> updateAuthRole(String uuid, @Valid ApiAuthRole body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
