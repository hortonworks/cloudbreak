package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.freeipa.api.v1.freeipa.test.model.ClientTestBaseV1Response.resultOf;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.freeipa.api.v1.freeipa.test.ClientTestV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckGroupsV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckUsersInGroupV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckUsersV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.test.model.ClientTestBaseV1Response;
import com.sequenceiq.freeipa.service.client.FreeipaClientTestService;

@Controller
public class ClientTestV1Controller implements ClientTestV1Endpoint {

    @Inject
    private FreeipaClientTestService freeipaClientTestService;

    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    @Override
    public String userShow(Long stackId, String userName) {
        return freeipaClientTestService.userShow(stackId, userName);
    }

    @Override
    @CheckPermissionByRequestProperty(type = CRN, action = DESCRIBE_ENVIRONMENT, path = "environmentCrn")
    public ClientTestBaseV1Response checkUsers(@RequestObject CheckUsersV1Request checkUsersRequest) {
        return resultOf(freeipaClientTestService.checkUsers(checkUsersRequest.getEnvironmentCrn(), checkUsersRequest.getUsers()));
    }

    @Override
    @CheckPermissionByRequestProperty(type = CRN, action = DESCRIBE_ENVIRONMENT, path = "environmentCrn")
    public ClientTestBaseV1Response checkGroups(@RequestObject CheckGroupsV1Request checkGroupsRequest) {
        return resultOf(freeipaClientTestService.checkGroups(checkGroupsRequest.getEnvironmentCrn(), checkGroupsRequest.getGroups()));
    }

    @Override
    @CheckPermissionByRequestProperty(type = CRN, action = DESCRIBE_ENVIRONMENT, path = "environmentCrn")
    public ClientTestBaseV1Response checkUsersInGroup(@RequestObject CheckUsersInGroupV1Request checkUsersInGroupRequest) {
        return resultOf(freeipaClientTestService.checkUsersInGroup(checkUsersInGroupRequest.getEnvironmentCrn(),
                checkUsersInGroupRequest.getUsers(), checkUsersInGroupRequest.getGroup()));
    }
}
