package com.sequenceiq.cloudbreak.sdx.common.service;

public interface PlatformAwareSdxStartStopService extends PlatformAwareSdxCommonService {
    void startSdx(String sdxCrn);

    void stopSdx(String sdxCrn);
}
