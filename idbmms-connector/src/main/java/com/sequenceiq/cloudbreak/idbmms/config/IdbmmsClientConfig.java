package com.sequenceiq.cloudbreak.idbmms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.netty.util.internal.StringUtil;

@Configuration
public class IdbmmsClientConfig {

    private final String callingServiceName;

    @Autowired
    public IdbmmsClientConfig(@Value("${altus.idbmms.caller:cloudbreak}") String callingServiceName) {
        this.callingServiceName = callingServiceName;
    }

    public String getCallingServiceName() {
        return callingServiceName;
    }

    public boolean isConfigured() {
        return !StringUtil.isNullOrEmpty(callingServiceName);
    }
}