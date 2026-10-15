package com.sequenceiq.it.cloudbreak.assertion.safelogic;

import java.util.Collection;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.assertion.util.InstanceIPCollectorUtil;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshSafeLogicActions;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@Component
public class SafeLogicAssertions {

    @Inject
    private SshSafeLogicActions sshSafeLogicActions;

    public void validate(TestContext testContext) {
        SdxTestDto sdxTestDto = testContext.get(SdxTestDto.class);
        if (sdxTestDto != null && shouldValidateSafeLogic(sdxTestDto.getResponse())) {
            validate(testContext, getSdxIpAddresses(sdxTestDto));
        }
        SdxInternalTestDto sdxInternalTestDto = testContext.get(SdxInternalTestDto.class);
        if (sdxInternalTestDto != null && shouldValidateSafeLogic(sdxInternalTestDto.getResponse())) {
            validate(testContext, getSdxIpAddresses(sdxInternalTestDto));
        }
        DistroXTestDto distroXTestDto = testContext.get(DistroXTestDto.class);
        if (distroXTestDto != null && shouldValidateSafeLogic(distroXTestDto.getResponse())) {
            validate(testContext, getDistroXIpAddresses(distroXTestDto));
        }
    }

    private boolean shouldValidateSafeLogic(SdxClusterDetailResponse sdxClusterDetailResponse) {
        return sdxClusterDetailResponse != null && shouldValidateSafeLogic(sdxClusterDetailResponse.getStackV4Response());
    }

    private boolean shouldValidateSafeLogic(StackV4Response stackV4Response) {
        return stackV4Response != null && stackV4Response.getJavaVersion() == 8;
    }

    private void validate(TestContext testContext, Collection<String> ipAddresses) {
        boolean govCloud = testContext.getCloudProvider().getGovCloud();
        if (govCloud != sshSafeLogicActions.hasSafeLogicBinaries(ipAddresses)) {
            throw new TestFailException(govCloud ? "Expected SafeLogic binaries but not found all of them!" : "Expected no SafeLogic binaries but found them!");
        }
        if (govCloud != sshSafeLogicActions.hasCryptoComplyForJavaSecurityProvider(ipAddresses)) {
            throw new TestFailException(govCloud ? "Expected CryptoComplyForJava to be configured" : "Expected CryptoComplyForJava to not be configured");
        }
        if (govCloud) {
            sshSafeLogicActions.validateMaxAESKeyLength(ipAddresses);
        }
    }

    private static List<String> getSdxIpAddresses(SdxInternalTestDto testDto) {
        return InstanceIPCollectorUtil.getGatewayIps(testDto.getResponse().getStackV4Response().getInstanceGroups(), false);
    }

    private static List<String> getSdxIpAddresses(SdxTestDto testDto) {
        return InstanceIPCollectorUtil.getGatewayIps(testDto.getResponse().getStackV4Response().getInstanceGroups(), false);
    }

    private static List<String> getDistroXIpAddresses(DistroXTestDto testDto) {
        return InstanceIPCollectorUtil.getGatewayIps(testDto.getResponse().getInstanceGroups(), false);
    }
}
