package com.sequenceiq.mock.clouderamanager.base;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiUser2;
import com.sequenceiq.mock.swagger.model.ApiUser2List;

@Controller
public class UsersResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    public ResponseEntity<ApiUser2List> createUsers2(String mockUuid, @Valid ApiUser2List body) {
        ApiUser2List apiUser2List = clouderaManagerStoreService.addUsers(mockUuid, body);
        return responseCreatorComponent.exec(apiUser2List);
    }

    public ResponseEntity<ApiUser2List> readUsers2(String mockUuid, @Valid String view) {
        return responseCreatorComponent.exec(clouderaManagerStoreService.getUserList(mockUuid));
    }

    public ResponseEntity<ApiUser2> updateUser2(String mockUuid, String userName, @Valid ApiUser2 body) {
        ApiUser2 user = clouderaManagerStoreService.updateUser(mockUuid, userName, body);
        return responseCreatorComponent.exec(user);
    }

    public ResponseEntity<ApiUser2> deleteUser2(String mockUuid, String userName) {
        ApiUser2 apiUser2 = clouderaManagerStoreService.removeUser(mockUuid, userName);
        return responseCreatorComponent.exec(apiUser2);
    }
}
