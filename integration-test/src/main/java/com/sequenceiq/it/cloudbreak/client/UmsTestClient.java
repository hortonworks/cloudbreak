package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.ums.AddUserToGroupAction;
import com.sequenceiq.it.cloudbreak.action.ums.AssignResourceRoleAction;
import com.sequenceiq.it.cloudbreak.action.ums.CreateUserGroupAction;
import com.sequenceiq.it.cloudbreak.action.ums.DeleteUserGroupAction;
import com.sequenceiq.it.cloudbreak.action.ums.GetUserDetailsAction;
import com.sequenceiq.it.cloudbreak.action.ums.ListGroupMembersAction;
import com.sequenceiq.it.cloudbreak.action.ums.SetWorkloadPasswordAction;
import com.sequenceiq.it.cloudbreak.action.ums.UnassignResourceRoleAction;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;

@Service
public class UmsTestClient {

    public Action<UmsTestDto, UmsClient> assignResourceRole(String userKey) {
        return new AssignResourceRoleAction(userKey);
    }

    public Action<UmsTestDto, UmsClient> unAssignResourceRole(String userKey) {
        return new UnassignResourceRoleAction(userKey);
    }

    public Action<UmsTestDto, UmsClient> setWorkloadPassword(String newPassword) {
        return new SetWorkloadPasswordAction(newPassword);
    }

    public Action<UmsTestDto, UmsClient> getUserDetails(String actorCrn, String userCrn) {
        return new GetUserDetailsAction(actorCrn, userCrn);
    }

    public Action<UmsGroupTestDto, UmsClient> createUserGroup(String groupName) {
        return new CreateUserGroupAction(groupName);
    }

    public Action<UmsGroupTestDto, UmsClient> deleteUserGroup(String groupName) {
        return new DeleteUserGroupAction(groupName);
    }

    public Action<UmsGroupTestDto, UmsClient> addUserToGroup(String userName, String memberCrn) {
        return new AddUserToGroupAction(userName, memberCrn);
    }

    public Action<UmsGroupTestDto, UmsClient> listGroupMembers(String groupName) {
        return new ListGroupMembersAction(groupName);
    }
}
