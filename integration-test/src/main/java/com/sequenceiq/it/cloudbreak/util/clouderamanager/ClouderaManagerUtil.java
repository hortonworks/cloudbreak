package com.sequenceiq.it.cloudbreak.util.clouderamanager;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.action.ClouderaManagerClientActions;

@Component
public class ClouderaManagerUtil {

    @Inject
    private ClouderaManagerClientActions clouderaManagerClientActions;

    private ClouderaManagerUtil() {
    }

    public SdxInternalTestDto checkClouderaManagerKnoxIDBrokerRoleConfigGroups(SdxInternalTestDto testDto, String user, String password) {
        return clouderaManagerClientActions.checkCmKnoxIDBrokerRoleConfigGroups(testDto, user, password);
    }
}
