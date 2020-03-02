package com.sequenceiq.environment.util.v1;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.common.api.util.versionchecker.ClientVersionUtil;
import com.sequenceiq.common.api.util.versionchecker.VersionCheckResult;
import com.sequenceiq.environment.api.v1.util.endpoint.UtilEndpoint;

@Controller
@DisableCheckPermissions
public class UtilController implements UtilEndpoint {

    @Value("${info.app.version:}")
    private String envVersion;

    @Override
    public VersionCheckResult checkClientVersion(String version) {
        return ClientVersionUtil.checkClientVersion(envVersion, version);
    }
}
