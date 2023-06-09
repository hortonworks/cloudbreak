package com.sequenceiq.it.cloudbreak.assertion.safelogic;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshSafeLogicActions;

@Component
public class SafeLogicAssertions {

    @Inject
    private SshSafeLogicActions sshSafeLogicActions;

    public void validate(TestContext testContext) {
        SdxTestDto sdxTestDto = testContext.get(SdxTestDto.class);
        if (sdxTestDto != null && sdxTestDto.getResponse() != null) {
            validate(testContext, getSdxIpAddresses(sdxTestDto));
        }
        SdxInternalTestDto sdxInternalTestDto = testContext.get(SdxInternalTestDto.class);
        if (sdxInternalTestDto != null && sdxInternalTestDto.getResponse() != null) {
            validate(testContext, getSdxIpAddresses(sdxInternalTestDto));
        }
        DistroXTestDto distroXTestDto = testContext.get(DistroXTestDto.class);
        if (distroXTestDto != null && distroXTestDto.getResponse() != null) {
            validate(testContext, getDistroXIpAddresses(distroXTestDto));
        }
    }

    private void validate(TestContext testContext, Set<String> ipAddresses) {
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

    private static Set<String> getSdxIpAddresses(SdxInternalTestDto testDto) {
        return getStackIpAddresses(testDto.getResponse().getStackV4Response());
    }

    private static Set<String> getSdxIpAddresses(SdxTestDto testDto) {
        return getStackIpAddresses(testDto.getResponse().getStackV4Response());
    }

    private static Set<String> getDistroXIpAddresses(DistroXTestDto testDto) {
        return getStackIpAddresses(testDto.getResponse());
    }

    private static Set<String> getStackIpAddresses(StackV4Response stack) {
        return stack.getInstanceGroups().stream()
                .filter(ig -> ig.getType().equals(InstanceGroupType.GATEWAY))
                .flatMap(ig -> ig.getMetadata().stream())
                .map(InstanceMetaDataV4Response::getPrivateIp)
                .collect(Collectors.toSet());
    }
}
