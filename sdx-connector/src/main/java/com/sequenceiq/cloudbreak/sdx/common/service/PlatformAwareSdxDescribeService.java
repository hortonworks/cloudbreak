package com.sequenceiq.cloudbreak.sdx.common.service;

import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;

public interface PlatformAwareSdxDescribeService extends PlatformAwareSdxCommonService {

    Optional<String> getRemoteDataContext(String crn);

    Set<String> listSdxCrns(String environmentCrn);

    Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn);

    boolean isSdxClusterHA(String environmentCrn);
}
