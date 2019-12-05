package com.sequenceiq.it.cloudbreak.testcase.e2e.gcp;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.CloudFunctionality;

public class GcpCloudFunctionality implements CloudFunctionality {

    private static final String GCP_IMPLEMENTATION_MISSING = "GCP implementation missing";

    @Override
    public List<String> listHostGroupVolumeIds(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public SdxTestDto compareVolumeIds(SdxTestDto sdxTestDto, List<String> actualVolumeIds, List<String> expectedVolumeIds) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public SdxTestDto deleteHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public SdxTestDto stopHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }

    @Override
    public SdxInternalTestDto stopHostGroupInstances(TestContext testContext, SdxInternalTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        throw new NotImplementedException(GCP_IMPLEMENTATION_MISSING);
    }
}
