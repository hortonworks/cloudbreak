package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.ums.AddUserToGroupAction;
import com.sequenceiq.it.cloudbreak.action.ums.AssignResourceRoleGroupAction;
import com.sequenceiq.it.cloudbreak.action.ums.AssignResourceRoleUserAction;
import com.sequenceiq.it.cloudbreak.action.ums.CreateUserGroupAction;
import com.sequenceiq.it.cloudbreak.action.ums.DeleteUserGroupAction;
import com.sequenceiq.it.cloudbreak.action.ums.GetUserDetailsAction;
import com.sequenceiq.it.cloudbreak.action.ums.ListGroupMembersAction;
import com.sequenceiq.it.cloudbreak.action.ums.RemoveUserFromGroupAction;
import com.sequenceiq.it.cloudbreak.action.ums.SetWorkloadPasswordAction;
import com.sequenceiq.it.cloudbreak.action.ums.UnassignResourceRoleAction;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;

@Service
public class UmsTestClient {

    public Action<UmsTestDto, UmsClient> assignResourceRole(String userKey,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new AssignResourceRoleUserAction(userKey, regionAwareInternalCrnGeneratorFactory);
    }

    public Action<UmsTestDto, UmsClient> unAssignResourceRole(String userKey,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new UnassignResourceRoleAction(userKey, regionAwareInternalCrnGeneratorFactory);
    }

    public Action<UmsTestDto, UmsClient> assignResourceRoleWithGroup(String groupCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new AssignResourceRoleGroupAction(groupCrn, regionAwareInternalCrnGeneratorFactory);
    }

    public Action<UmsTestDto, UmsClient> setWorkloadPassword(String newPassword,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new SetWorkloadPasswordAction(newPassword, regionAwareInternalCrnGeneratorFactory);
    }

    public Action<UmsTestDto, UmsClient> getUserDetails(String userCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new GetUserDetailsAction(userCrn, regionAwareInternalCrnGeneratorFactory);
    }

    public Action<UmsGroupTestDto, UmsClient> createUserGroup(String groupName,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new CreateUserGroupAction(groupName, regionAwareInternalCrnGeneratorFactory);
    }

    public Action<UmsGroupTestDto, UmsClient> deleteUserGroup(String groupName,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new DeleteUserGroupAction(groupName, regionAwareInternalCrnGeneratorFactory);
    }

    public Action<UmsGroupTestDto, UmsClient> addUserToGroup(String groupName, String memberCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new AddUserToGroupAction(groupName, memberCrn, regionAwareInternalCrnGeneratorFactory);
    }

    public Action<UmsGroupTestDto, UmsClient> removeUserFromGroup(String groupName, String memberCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new RemoveUserFromGroupAction(groupName, memberCrn, regionAwareInternalCrnGeneratorFactory);
    }

    public Action<UmsGroupTestDto, UmsClient> listGroupMembers(String groupName,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new ListGroupMembersAction(groupName, regionAwareInternalCrnGeneratorFactory);
    }
}
