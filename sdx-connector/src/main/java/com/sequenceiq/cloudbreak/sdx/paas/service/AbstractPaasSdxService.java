package com.sequenceiq.cloudbreak.sdx.paas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxCommonService;

public abstract class AbstractPaasSdxService implements PlatformAwareSdxCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPaasSdxService.class);

    @Override
    public TargetPlatform targetPlatform() {
        return TargetPlatform.PAAS;
    }

    @Override
    public boolean isPlatformEntitled(String accountId) {
        return true;
    }
}
