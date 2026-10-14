package com.sequenceiq.cloudbreak.sdx;

import org.springframework.stereotype.Service;

@Service
public class OnPrem719WorkaroundService {

    private static final String ON_PREM_RUNTIME_VERSION = "7.1.9";

    private static final String REPLACEMENT_RUNTIME_VERSION = "7.3.2";

    public String replaceRdcRuntimeVersion(String rdcJson) {
        return rdcJson.replaceAll("CDH 7\\.1\\.9", "CDH " + REPLACEMENT_RUNTIME_VERSION);
    }

    public String replaceRuntimeVersion(String runtimeVersion) {
        return ON_PREM_RUNTIME_VERSION.equals(runtimeVersion) ? REPLACEMENT_RUNTIME_VERSION : runtimeVersion;
    }
}
