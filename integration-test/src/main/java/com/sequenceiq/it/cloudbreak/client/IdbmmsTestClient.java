package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.idbmms.IdbmmsDeleteAction;
import com.sequenceiq.it.cloudbreak.action.idbmms.IdbmmsGetAction;
import com.sequenceiq.it.cloudbreak.action.idbmms.IdbmmsSetAction;
import com.sequenceiq.it.cloudbreak.dto.idbmms.IdbmmsTestDto;
import com.sequenceiq.it.cloudbreak.microservice.IdbmmsClient;

@Service
public class IdbmmsTestClient {

    public Action<IdbmmsTestDto, IdbmmsClient> set(String dataAccessRole, String baselineRole, String rangerAccessAuthorizerRole) {
        return new IdbmmsSetAction(dataAccessRole, baselineRole, rangerAccessAuthorizerRole);
    }

    public Action<IdbmmsTestDto, IdbmmsClient> delete() {
        return new IdbmmsDeleteAction();
    }

    public Action<IdbmmsTestDto, IdbmmsClient> get() {
        return new IdbmmsGetAction();
    }
}
