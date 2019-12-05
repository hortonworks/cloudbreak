package com.sequenceiq.it.cloudbreak.testcase.e2e;

import java.util.List;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

/**
 * REFACTOR NOTE:
 *
 * Interface for all methods interacting with the cloud provider, needed in tests
 * This should hide all the differences between cloud providers from the concrete tests.
 *
 * Implementations of this interface would call corresponding Util classes (like S3Util when calling cloud storage functionality on aws)
 *
 * Later on, when all functionality is incorporated into CloudFunctionality, the intermediate *Util classes could be left out.
 */
public interface CloudFunctionality {
    List<String> listHostGroupVolumeIds(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName);

    SdxTestDto compareVolumeIds(SdxTestDto sdxTestDto, List<String> actualVolumeIds, List<String> expectedVolumeIds);

    SdxTestDto deleteHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName);

    SdxTestDto stopHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName);

    SdxInternalTestDto stopHostGroupInstances(TestContext testContext, SdxInternalTestDto testDto, SdxClient sdxClient, String hostGroupName);

    /*
    *
    * Here
    *
    * */
}
