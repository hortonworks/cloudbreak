package com.sequenceiq.cloudbreak.sdx.common.service;

import com.sequenceiq.cloudbreak.sdx.TargetPlatform;

public interface PlatformAwareSdxCommonService {

    TargetPlatform targetPlatform();

    boolean isPlatformEntitled(String accountId);
}
