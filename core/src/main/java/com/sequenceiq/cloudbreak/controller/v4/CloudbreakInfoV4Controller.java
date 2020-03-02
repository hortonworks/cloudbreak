package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;

import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.info.CloudbreakInfoV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.info.responses.CloudbreakInfoResponse;

@Controller
@DisableCheckPermissions
public class CloudbreakInfoV4Controller extends NotificationController implements CloudbreakInfoV4Endpoint {

    @Inject
    private InfoEndpoint infoEndpoint;

    @Override
    public CloudbreakInfoResponse info() {
        CloudbreakInfoResponse cloudbreakInfoResponse = new CloudbreakInfoResponse();
        cloudbreakInfoResponse.setInfo(infoEndpoint.info());
        return cloudbreakInfoResponse;
    }
}
