package com.sequenceiq.mock.legacy.clouderamanager.v40.controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DataProviderService;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.swagger.v40.api.UsersResourceApi;
import com.sequenceiq.mock.swagger.model.ApiUser2;
import com.sequenceiq.mock.swagger.model.ApiUser2List;
import com.sequenceiq.mock.swagger.model.ApiUserSessionList;

@Controller
public class UsersResourceV40Controller implements UsersResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<ApiUser2List> createUsers2(@Valid ApiUser2List body) {
        return ProfileAwareResponse.exec(dataProviderService.getUserList(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiUser2> deleteUser2(String userName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Void> expireSessions(String userName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiUserSessionList> getSessions() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiUser2> readUser2(String userName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiUser2List> readUsers2(@Valid String view) {
        return ProfileAwareResponse.exec(dataProviderService.getUserList(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiUser2> updateUser2(String userName, @Valid ApiUser2 body) {
        return ProfileAwareResponse.exec(new ApiUser2().name(userName), defaultModelService);
    }
}
