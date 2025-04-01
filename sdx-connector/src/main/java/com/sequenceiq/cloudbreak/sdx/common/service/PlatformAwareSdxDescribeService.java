package com.sequenceiq.cloudbreak.sdx.common.service;

import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;

public interface PlatformAwareSdxDescribeService extends PlatformAwareSdxCommonService {

    Optional<String> getRemoteDataContext(String crn);

    default RdcView extendRdcView(RdcView rdcView) {
        // do nothing
        return rdcView;
    }

    Set<String> listSdxCrns(String environmentCrn);

    Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn);

    Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn);

    Set<String> listSdxCrnsDetachedIncluded(String environmentCrn);
}
