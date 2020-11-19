package com.sequenceiq.mock.clouderamanager.v40.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiUser2;
import com.sequenceiq.mock.swagger.model.ApiUser2List;
import com.sequenceiq.mock.swagger.v40.api.UsersResourceApi;

@Controller
public class UsersResourceV40Controller implements UsersResourceApi {

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<ApiUser2List> createUsers2(String mockUuid, @Valid ApiUser2List body) {
        return profileAwareComponent.exec(dataProviderService.getUserList());
    }

    @Override
    public ResponseEntity<ApiUser2List> readUsers2(String mockUuid, @Valid String view) {
        return profileAwareComponent.exec(dataProviderService.getUserList());
    }

    @Override
    public ResponseEntity<ApiUser2> updateUser2(String mockUuid, String userName, @Valid ApiUser2 body) {
        return profileAwareComponent.exec(new ApiUser2().name(userName));
    }
}
