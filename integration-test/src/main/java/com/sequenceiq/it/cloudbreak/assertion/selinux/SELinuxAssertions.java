package com.sequenceiq.it.cloudbreak.assertion.selinux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.SeLinux;
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
public class SELinuxAssertions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SELinuxAssertions.class);

    private static final String CHECK_SELINUX_MODE_COMMAND = "getenforce | tr -d '[:space:]'";

    private static final String CHECK_CDP_MODULE_COUNT_COMMAND = """
            cdp_module_count=$(ls -1d /etc/selinux/cdp/*/ | wc -l); \
            if [[ -f /etc/selinux/cdp/cdp-policy-installer.te ]]; then \
                echo "Including cdp-policy-installer module in the expected ";
                ((cdp_module_count++)); \
            fi; \
            installed_cdp_module_count=$(semodule -l | grep "^cdp-" | wc -l); \
            echo; \
            echo "Expected count of installed CDP modules: $cdp_module_count"; \
            echo "Actual count of installed CDP modules: $installed_cdp_module_count"; \
            [[ $cdp_module_count -eq $installed_cdp_module_count ]]\
            """;

    private static final String CHECK_ANY_DENIES_COMMAND = """
            count=$(ausearch -m AVC,SELINUX_ERR,USER_SELINUX_ERR | grep 'time->' | wc -l); \
            if [ "$count" -ne 0 ]; then \
                echo; \
                echo "Found $count deny log(s)."; \
                echo "=== Deny Logs (Human Readable) ==="; \
                ausearch -m AVC,SELINUX_ERR,USER_SELINUX_ERR -i; \
                echo; \
                echo "=== audit2allow Output ==="; \
                ausearch -m AVC,SELINUX_ERR,USER_SELINUX_ERR | audit2allow; \
                echo; \
                exit "$count"; \
            else \
                echo "No deny logs found."; \
                exit 0; \
            fi\
            """;

    @Inject
    private SshSudoCommandActions sshSudoCommandActions;

    public void validateAllExistingAndThrowIfAnyError(TestContext testContext) {
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        FreeIpaTestDto freeIpaTestDto = testContext.get(FreeIpaTestDto.class);
        if (freeIpaTestDto != null) {
            FreeIpaClient freeIpaClient = testContext.getMicroserviceClient(FreeIpaClient.class);
            SeLinux expectedSelinuxMode = freeIpaTestDto.getSelinuxMode();
            List<String> instanceIps = InstanceIPCollectorUtil.getAllInstanceIps(freeIpaTestDto, freeIpaClient, false);
            validateAll(validationBuilder, "FreeIpa", instanceIps, expectedSelinuxMode);
        }

        SdxInternalTestDto sdxInternalTestDto = testContext.get(SdxInternalTestDto.class);
        if (sdxInternalTestDto != null) {
            SdxClient sdxClient = testContext.getMicroserviceClient(SdxClient.class);
            SeLinux expectedSelinuxMode = sdxInternalTestDto.getSelinuxMode();
            List<String> instanceIps = InstanceIPCollectorUtil.getAllInstanceIps(sdxInternalTestDto, sdxClient, false);
            validateAll(validationBuilder, "DataLake", instanceIps, expectedSelinuxMode);
        }

        DistroXTestDto distroXTestDto = testContext.get(DistroXTestDto.class);
        if (distroXTestDto != null) {
            CloudbreakClient cloudbreakClient = testContext.getMicroserviceClient(CloudbreakClient.class);
            SeLinux expectedSelinuxMode = distroXTestDto.getSelinuxMode();
            List<String> instanceIps = InstanceIPCollectorUtil.getAllInstanceIps(distroXTestDto, cloudbreakClient, false);
            validateAll(validationBuilder, "DataHub", instanceIps, expectedSelinuxMode);
        }

        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new TestFailException(validationResult.getFormattedErrors());
        }
    }

    public FreeIpaTestDto validateAllAndThrowIfAnyError(TestContext testContext, FreeIpaTestDto freeIpaTestDto, FreeIpaClient freeIpaClient) {
        List<String> instanceIps = InstanceIPCollectorUtil.getAllInstanceIps(freeIpaTestDto, freeIpaClient, false);
        ValidationResult validationResult = validateAll("FreeIPA", instanceIps, freeIpaTestDto.getSelinuxMode()).build();
        if (validationResult.hasError()) {
            throw new TestFailException(validationResult.getFormattedErrors());
        }
        return freeIpaTestDto;
    }

    public SdxInternalTestDto validateAllAndThrowIfAnyError(TestContext testContext, SdxInternalTestDto sdxInternalTestDto, SdxClient sdxClient) {
        List<String> instanceIps = InstanceIPCollectorUtil.getAllInstanceIps(sdxInternalTestDto, sdxClient, false);
        ValidationResult validationResult = validateAll("DataLake", instanceIps, sdxInternalTestDto.getSelinuxMode()).build();
        if (validationResult.hasError()) {
            throw new TestFailException(validationResult.getFormattedErrors());
        }
        return sdxInternalTestDto;
    }

    public DistroXTestDto validateAllAndThrowIfAnyError(TestContext testContext, DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        List<String> instanceIps = InstanceIPCollectorUtil.getAllInstanceIps(distroXTestDto, cloudbreakClient, false);
        ValidationResult validationResult = validateAll("DataHub", instanceIps, distroXTestDto.getSelinuxMode()).build();
        if (validationResult.hasError()) {
            throw new TestFailException(validationResult.getFormattedErrors());
        }
        return distroXTestDto;
    }

    public ValidationResult.ValidationResultBuilder validateAll(String stackType, List<String> instanceIps, SeLinux expectedSelinuxMode) {
        return validateAll(null, stackType, instanceIps, expectedSelinuxMode);
    }

    private ValidationResult.ValidationResultBuilder validateAll(ValidationResult.ValidationResultBuilder validationBuilder,
            String stackType, List<String> instanceIps, SeLinux expectedSelinuxMode) {
        ValidationResult.ValidationResultBuilder builder = validationBuilder == null ? ValidationResult.builder() : validationBuilder;

        Map<String, String> instancesWithUnexpectedMode = getInstancesWithUnexpectedMode(instanceIps, expectedSelinuxMode);
        if (!instancesWithUnexpectedMode.isEmpty()) {
            builder.error(
                    String.format("The SELinux validation found %s instances with unexpected SELinux mode. Expected: %s. Instances with unexpected mode: %s",
                            stackType, expectedSelinuxMode.name(), instancesWithUnexpectedMode));
        }

        Map<String, String> instancesWithMissingModules = getInstancesWithMissingModules(instanceIps);
        if (!instancesWithMissingModules.isEmpty()) {
            builder.error(
                    String.format("The SELinux validation found %s instances with missing CDP modules. Instances with missing modules: %s",
                            stackType, instancesWithMissingModules));
        }

        Map<String, String> instancesWithAuditedDenies = getInstancesWithAuditedDenies(instanceIps);
        if (!instancesWithAuditedDenies.isEmpty()) {
            builder.error(
                    String.format("The SELinux validation found denies on some %s instances. Instances with denies in their audit log: %s",
                            stackType, instancesWithAuditedDenies));
        }

        return builder;
    }

    private Map<String, String> getInstancesWithUnexpectedMode(List<String> instanceIps, SeLinux expectedSELinuxMode) {
        Map<String, Pair<Integer, String>> commandOutputs = sshSudoCommandActions.executeCommandWithoutThrowing(instanceIps, CHECK_SELINUX_MODE_COMMAND);
        return commandOutputs.entrySet().stream()
                .filter(entry -> !expectedSELinuxMode.equals(SeLinux.fromStringWithFallback(entry.getValue().getRight())))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getRight()));
    }

    private Map<String, String> getInstancesWithMissingModules(List<String> instanceIps) {
        Map<String, Pair<Integer, String>> commandOutputs = sshSudoCommandActions.executeCommandWithoutThrowing(instanceIps, CHECK_CDP_MODULE_COUNT_COMMAND);
        return commandOutputs.entrySet().stream()
                .filter(entry -> entry.getValue().getLeft() != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getRight()));
    }

    private Map<String, String> getInstancesWithAuditedDenies(List<String> instanceIps) {
        Map<String, Pair<Integer, String>> commandOutputs = sshSudoCommandActions.executeCommandWithoutThrowing(instanceIps, CHECK_ANY_DENIES_COMMAND);
        return commandOutputs.entrySet().stream()
                .filter(entry -> entry.getValue().getLeft() != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getRight()));
    }
}
