package com.sequenceiq.it.cloudbreak.assertion.encryption;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.assertion.util.InstanceIPCollectorUtil;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshSudoCommandActions;

@Component
public class SecretEncryptionAssertions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretEncryptionAssertions.class);

    private static final String SECRET_VOLUME_STATUS_CHECK = "cryptsetup status cdp-luks";

    private static final String SECRET_VOLUME_KEYSLOT_COUNT_CHECK =
            "test \"$(sudo cryptsetup luksDump \"$(losetup -j /etc/cdp-luks/cdp-luks | awk '{print $1}' | tr -d ':')\"" +
                    " | grep -cE '^\\s*[0-9]+:\\s+luks2')\" = \"1\"";

    private static final String BACKING_FILE_PERMISSION_CHECK = "test \"$(sudo stat -c '%a' /etc/cdp-luks/cdp-luks)\" = \"600\"";

    private static final String PASSPHRASE_CIPHERTEXT_PERMISSION_CHECK = "test \"$(sudo stat -c '%a' /etc/cdp-luks/passphrase_ciphertext)\" = \"600\"";

    private static final String PASSPHRASE_PLAINTEXT_TMPFS_PERMISSION_CHECK = "test \"$(sudo stat -c '%a' /mnt/cdp-luks_passphrase_tmpfs)\" = \"700\"";

    private static final List<String> ALL_COMMANDS = List.of(SECRET_VOLUME_STATUS_CHECK, SECRET_VOLUME_KEYSLOT_COUNT_CHECK,
            BACKING_FILE_PERMISSION_CHECK, PASSPHRASE_CIPHERTEXT_PERMISSION_CHECK, PASSPHRASE_PLAINTEXT_TMPFS_PERMISSION_CHECK);

    @Inject
    private SshSudoCommandActions sshSudoCommandActions;

    public FreeIpaTestDto validate(TestContext testContext, FreeIpaTestDto freeIpaTestDto, FreeIpaClient freeIpaClient) {
        if (shouldValidate(testContext)) {
            validate(freeIpaTestDto, freeIpaClient);
        } else {
            logValidationSkipped();
        }
        return freeIpaTestDto;
    }

    public SdxInternalTestDto validate(TestContext testContext, SdxInternalTestDto sdxInternalTestDto, SdxClient sdxClient) {
        if (shouldValidate(testContext)) {
            validate(sdxInternalTestDto, sdxClient);
        } else {
            logValidationSkipped();
        }
        return sdxInternalTestDto;
    }

    public DistroXTestDto validate(TestContext testContext, DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        if (shouldValidate(testContext)) {
            validate(distroXTestDto, cloudbreakClient);
        } else {
            logValidationSkipped();
        }
        return distroXTestDto;
    }

    public void validateAllExisting(TestContext testContext) {
        if (shouldValidate(testContext)) {
            FreeIpaTestDto freeIpaTestDto = testContext.get(FreeIpaTestDto.class);
            if (freeIpaTestDto != null) {
                validate(freeIpaTestDto, testContext.getMicroserviceClient(FreeIpaClient.class));
            }
            SdxInternalTestDto sdxInternalTestDto = testContext.get(SdxInternalTestDto.class);
            if (sdxInternalTestDto != null) {
                validate(sdxInternalTestDto, testContext.getMicroserviceClient(SdxClient.class));
            }
            DistroXTestDto distroXTestDto = testContext.get(DistroXTestDto.class);
            if (distroXTestDto != null) {
                validate(distroXTestDto, testContext.getMicroserviceClient(CloudbreakClient.class));
            }
        } else {
            logValidationSkipped();
        }
    }

    private boolean shouldValidate(TestContext testContext) {
        return testContext.getCloudProvider().getGovCloud();
    }

    private void logValidationSkipped() {
        LOGGER.info("The secret encryption validation is skipped because the environment is not a GovCloud environment.");
    }

    private void validate(FreeIpaTestDto freeIpaTestDto, FreeIpaClient freeIpaClient) {
        List<String> instanceIps = InstanceIPCollectorUtil.getAllInstanceIps(freeIpaTestDto, freeIpaClient, false);
        Map<String, String> failedInstancesWithCommandOutput = getFailedInstancesWithCommandOutput(instanceIps);
        if (!failedInstancesWithCommandOutput.isEmpty()) {
            throw new TestFailException("The secret encryption validation did not succeed on all the FreeIPA instances. Failed instances with command output: "
                    + failedInstancesWithCommandOutput);
        }
    }

    private void validate(SdxInternalTestDto sdxInternalTestDto, SdxClient sdxClient) {
        List<String> instanceIps = InstanceIPCollectorUtil.getAllInstanceIps(sdxInternalTestDto, sdxClient, false);
        Map<String, String> failedInstancesWithCommandOutput = getFailedInstancesWithCommandOutput(instanceIps);
        if (!failedInstancesWithCommandOutput.isEmpty()) {
            throw new TestFailException("The secret encryption validation did not succeed on all the SDX instances. Failed instances with command output: "
                    + failedInstancesWithCommandOutput);
        }
    }

    private void validate(DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        List<String> instanceIps = InstanceIPCollectorUtil.getAllInstanceIps(distroXTestDto, cloudbreakClient, false);
        Map<String, String> failedInstancesWithCommandOutput = getFailedInstancesWithCommandOutput(instanceIps);
        if (!failedInstancesWithCommandOutput.isEmpty()) {
            throw new TestFailException("The secret encryption validation did not succeed on all the DistroX instances. Failed instances with command output: "
                    + failedInstancesWithCommandOutput);
        }
    }

    private Map<String, String> getFailedInstancesWithCommandOutput(List<String> instanceIps) {
        Map<String, Pair<Integer, String>> commandOutputs = sshSudoCommandActions.executeCommandWithoutThrowing(instanceIps, ALL_COMMANDS);
        return commandOutputs.entrySet().stream()
                .filter(entry -> entry.getValue().getLeft() != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getRight()));
    }
}
