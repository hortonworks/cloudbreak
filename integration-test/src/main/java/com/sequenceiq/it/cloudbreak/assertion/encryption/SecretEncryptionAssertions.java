package com.sequenceiq.it.cloudbreak.assertion.encryption;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
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
        List<String> instanceIps = getAllFreeipaInstanceIps(freeIpaTestDto.getEnvironmentCrn(), freeIpaClient, false);
        Map<String, String> failedInstancesWithCommandOutput = getFailedInstancesWithCommandOutput(instanceIps);
        if (!failedInstancesWithCommandOutput.isEmpty()) {
            throw new TestFailException("The secret encryption validation did not succeed on all the FreeIPA instances. Failed instances with command output: "
                    + failedInstancesWithCommandOutput);
        }
    }

    private void validate(SdxInternalTestDto sdxInternalTestDto, SdxClient sdxClient) {
        List<String> instanceIps = getAllDataLakeInstanceIps(sdxInternalTestDto.getCrn(), sdxClient, false);
        Map<String, String> failedInstancesWithCommandOutput = getFailedInstancesWithCommandOutput(instanceIps);
        if (!failedInstancesWithCommandOutput.isEmpty()) {
            throw new TestFailException("The secret encryption validation did not succeed on all the SDX instances. Failed instances with command output: "
                    + failedInstancesWithCommandOutput);
        }
    }

    private void validate(DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        List<String> instanceIps = getAllDataHubInstanceIps(distroXTestDto.getCrn(), cloudbreakClient, false);
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

    private List<String> getAllFreeipaInstanceIps(String environmentCrn, FreeIpaClient freeipaClient, boolean publicIp) {
        return freeipaClient.getDefaultClient().getFreeIpaV1Endpoint().describe(environmentCrn).getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetaData().stream())
                .filter(Objects::nonNull)
                .map(instanceMetaData -> mapInstanceToIp(instanceMetaData, publicIp))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> getAllDataLakeInstanceIps(String datalakeCrn, SdxClient sdxClient, boolean publicIp) {
        return sdxClient.getDefaultClient().sdxEndpoint().getDetailByCrn(datalakeCrn, Set.of()).getStackV4Response().getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                .filter(Objects::nonNull)
                .map(instanceMetaData -> mapInstanceToIp(instanceMetaData, publicIp))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> getAllDataHubInstanceIps(String datahubCrn, CloudbreakClient cloudbreakClient, boolean publicIp) {
        return cloudbreakClient.getDefaultClient().distroXV1Endpoint().getByCrn(datahubCrn, Set.of()).getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                .filter(Objects::nonNull)
                .map(instanceMetaData -> mapInstanceToIp(instanceMetaData, publicIp))
                .filter(Objects::nonNull)
                .toList();
    }

    private String mapInstanceToIp(InstanceMetaDataResponse instanceMetaDataResponse, boolean publicIp) {
        LOGGER.info("The selected FreeIPA Instance Type [{}] and the available Private IP [{}] and Public IP [{}]. {} IP will be used!",
                instanceMetaDataResponse.getInstanceType(), instanceMetaDataResponse.getPrivateIp(), instanceMetaDataResponse.getPublicIp(),
                publicIp ? "Public" : "Private");
        return publicIp ? instanceMetaDataResponse.getPublicIp() : instanceMetaDataResponse.getPrivateIp();
    }

    private String mapInstanceToIp(InstanceMetaDataV4Response instanceMetaDataV4Response, boolean publicIp) {
        LOGGER.info("The selected Instance Type [{}] and the available Private IP [{}] and Public IP [{}]. {} IP will be used!",
                instanceMetaDataV4Response.getInstanceType(), instanceMetaDataV4Response.getPrivateIp(), instanceMetaDataV4Response.getPublicIp(),
                publicIp ? "Public" : "Private");
        return publicIp ? instanceMetaDataV4Response.getPublicIp() : instanceMetaDataV4Response.getPrivateIp();
    }

}
