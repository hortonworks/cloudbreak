package com.sequenceiq.mock.clouderamanager.base;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleMetadataList;

@Controller
public class AuthRolesResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    public ResponseEntity<ApiAuthRoleMetadataList> readAuthRolesMetadata(String mockUuid, @Valid String view) {
        return responseCreatorComponent.exec(new ApiAuthRoleMetadataList());
    }
}
