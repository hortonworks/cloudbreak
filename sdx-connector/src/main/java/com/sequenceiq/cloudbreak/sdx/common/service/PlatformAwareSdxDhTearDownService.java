package com.sequenceiq.cloudbreak.sdx.common.service;

public interface PlatformAwareSdxDhTearDownService extends PlatformAwareSdxCommonService {

    void tearDownDataHub(String sdxCrn, String datahubCrn);
}
