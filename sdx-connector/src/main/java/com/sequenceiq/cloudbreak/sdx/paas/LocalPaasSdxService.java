package com.sequenceiq.cloudbreak.sdx.paas;

import java.util.Optional;

import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;

public interface LocalPaasSdxService {

    Optional<SdxBasicView> getSdxBasicView(String environmentCrn);
}
