package com.sequenceiq.it.cloudbreak.util.amazonec2;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.amazonec2.action.EC2ClientActions;

@Component
public class AmazonEC2Util {
    @Inject
    private EC2ClientActions ec2ClientActions;

    private AmazonEC2Util() {
    }

    public List<String> listHostGroupVolumeIds(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return ec2ClientActions.getHostGroupVolumeIds(testContext, testDto, sdxClient, hostGroupName);
    }

    public SdxTestDto compareVolumeIds(SdxTestDto sdxTestDto, List<String> actualVolumeIds, List<String> expectedVolumeIds) {
        return ec2ClientActions.compareVolumeIdsAfterRepair(sdxTestDto, actualVolumeIds, expectedVolumeIds);
    }

    public SdxTestDto deleteHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return ec2ClientActions.deleteHostGroupInstances(testContext, testDto, sdxClient, hostGroupName);
    }

    public SdxTestDto stopHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return ec2ClientActions.stopHostGroupInstances(testContext, testDto, sdxClient, hostGroupName);
    }

    public SdxInternalTestDto stopHostGroupInstances(TestContext testContext, SdxInternalTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        return ec2ClientActions.stopHostGroupInstances(testContext, testDto, sdxClient, hostGroupName);
    }
}
