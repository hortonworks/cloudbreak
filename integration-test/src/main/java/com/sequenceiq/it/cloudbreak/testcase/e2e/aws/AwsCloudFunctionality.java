package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import java.util.List;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.amazonec2.AmazonEC2Util;

public class AwsCloudFunctionality implements CloudFunctionality {

    private AmazonEC2Util amazonEC2Util;

    @Override
    public List<String> listHostGroupVolumeIds(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return amazonEC2Util.listHostGroupVolumeIds(testContext, testDto, sdxClient, hostGroupName);
    }

    @Override
    public SdxTestDto compareVolumeIds(SdxTestDto sdxTestDto, List<String> actualVolumeIds, List<String> expectedVolumeIds) {

        return amazonEC2Util.compareVolumeIds(sdxTestDto, actualVolumeIds, expectedVolumeIds);
    }

    @Override
    public SdxTestDto deleteHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return amazonEC2Util.deleteHostGroupInstances(testContext, testDto, sdxClient, hostGroupName);
    }

    @Override
    public SdxTestDto stopHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return amazonEC2Util.stopHostGroupInstances(testContext, testDto, sdxClient, hostGroupName);
    }

    @Override
    public SdxInternalTestDto stopHostGroupInstances(TestContext testContext, SdxInternalTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return amazonEC2Util.stopHostGroupInstances(testContext, testDto, sdxClient, hostGroupName);
    }
}
