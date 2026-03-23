package com.sequenceiq.cloudbreak.sdx;

import org.springframework.stereotype.Service;

@Service
public class OnPrem719WorkaroundService {

    public String replaceRdcRuntimeVersion(String rdcJson) {
        return rdcJson.replaceAll("CDH 7\\.1\\.9", "CDH 7.3.1");
    }

    public String replaceRuntimeVersion(String runtimeVersion) {
        return "7.1.9".equals(runtimeVersion) ? "7.3.1" : runtimeVersion;
    }
}
