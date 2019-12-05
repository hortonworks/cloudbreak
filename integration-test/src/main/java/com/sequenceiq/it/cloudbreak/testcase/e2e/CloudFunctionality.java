package com.sequenceiq.it.cloudbreak.testcase.e2e;

import java.util.List;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

public interface CloudFunctionality {
    List<String> listHostGroupVolumeIds(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName);

    SdxTestDto compareVolumeIds(SdxTestDto sdxTestDto, List<String> actualVolumeIds, List<String> expectedVolumeIds);

    SdxTestDto deleteHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName);

    SdxTestDto stopHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName);

    SdxInternalTestDto stopHostGroupInstances(TestContext testContext, SdxInternalTestDto testDto, SdxClient sdxClient, String hostGroupName);
}
