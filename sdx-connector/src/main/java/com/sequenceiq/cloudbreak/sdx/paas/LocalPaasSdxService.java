package com.sequenceiq.cloudbreak.sdx.paas;

import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxFileSystemView;

public interface LocalPaasSdxService {

    Optional<SdxBasicView> getSdxBasicView(String environmentCrn);

    Optional<SdxFileSystemView> getSdxFileSystemView(String environmentCrn);

    Optional<SdxAccessView> getSdxAccessView(String environmentCrn);

    Set<String> listSdxCrns(String environmentCrn);
}
