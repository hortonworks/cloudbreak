package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.ums.AssignUmsRoleAction;
import com.sequenceiq.it.cloudbreak.action.ums.GetUserDetailsAction;
import com.sequenceiq.it.cloudbreak.action.ums.SetWorkloadPasswordAction;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;

@Service
public class UmsTestClient {

    public Action<UmsTestDto, UmsClient> assignResourceRole(String userKey) {
        return new AssignUmsRoleAction(userKey);
    }

    public Action<UmsTestDto, UmsClient> setWorkloadPassword(String newPassword) {
        return new SetWorkloadPasswordAction(newPassword);
    }

    public Action<UmsTestDto, UmsClient> getUserDetails() {
        return new GetUserDetailsAction();
    }
}
