package com.sequenceiq.mock.clouderamanager.v40.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleMetadataList;
import com.sequenceiq.mock.swagger.v40.api.AuthRolesResourceApi;

@Controller
public class AuthRolesResourceV40Controller implements AuthRolesResourceApi {

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Override
    public ResponseEntity<ApiAuthRoleMetadataList> readAuthRolesMetadata(String mockUuid, @Valid String view) {
        return profileAwareComponent.exec(new ApiAuthRoleMetadataList());
    }
}
