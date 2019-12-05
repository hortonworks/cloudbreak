package com.sequenceiq.datalake.controller.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.sequenceiq.common.api.util.versionchecker.ClientVersionUtil;
import com.sequenceiq.common.api.util.versionchecker.VersionCheckResult;
import com.sequenceiq.sdx.api.endpoint.UtilEndpoint;

@Controller
public class UtilController implements UtilEndpoint {

    @Value("${info.app.version:}")
    private String sdxVersion;

    @Override
    public VersionCheckResult checkClientVersion(String version) {
        return ClientVersionUtil.checkClientVersion(sdxVersion, version);
    }
}
