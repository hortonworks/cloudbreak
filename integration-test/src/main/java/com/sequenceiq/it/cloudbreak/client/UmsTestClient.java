package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.ums.AssignResourceRoleAction;
import com.sequenceiq.it.cloudbreak.action.ums.GetUserDetailsAction;
import com.sequenceiq.it.cloudbreak.action.ums.SetWorkloadPasswordAction;
import com.sequenceiq.it.cloudbreak.action.ums.UnassignResourceRoleAction;
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
}
