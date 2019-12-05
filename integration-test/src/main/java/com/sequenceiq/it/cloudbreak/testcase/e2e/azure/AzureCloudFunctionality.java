package com.sequenceiq.it.cloudbreak.testcase.e2e.azure;

import java.util.List;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.CloudFunctionality;

public class AzureCloudFunctionality implements CloudFunctionality {
    @Override
    public List<String> listHostGroupVolumeIds(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return null;
    }

    @Override
    public SdxTestDto compareVolumeIds(SdxTestDto sdxTestDto, List<String> actualVolumeIds, List<String> expectedVolumeIds) {
        return null;
    }

    @Override
    public SdxTestDto deleteHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return null;
    }

    @Override
    public SdxTestDto stopHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return null;
    }

    @Override
    public SdxInternalTestDto stopHostGroupInstances(TestContext testContext, SdxInternalTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return null;
    }
}
