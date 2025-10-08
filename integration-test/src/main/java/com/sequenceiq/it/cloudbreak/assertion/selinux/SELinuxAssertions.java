package com.sequenceiq.it.cloudbreak.assertion.selinux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
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

    private static final String REPORT_DIRECTORY = "selinux-reports";

    @Inject
    private SshSudoCommandActions sshSudoCommandActions;

    public void validateAllExisting(TestContext testContext, boolean throwIfAnyError, boolean generateDenyReports) {
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        FreeIpaTestDto freeIpaTestDto = testContext.get(FreeIpaTestDto.class);
        if (freeIpaTestDto != null) {
            validateAll(validationBuilder, testContext, "FreeIpa",
                    freeIpaTestDto.getAllInstanceIps(testContext), freeIpaTestDto.getSelinuxMode(), generateDenyReports);
        }

        SdxInternalTestDto sdxInternalTestDto = testContext.get(SdxInternalTestDto.class);
        if (sdxInternalTestDto != null) {
            validateAll(validationBuilder, testContext, "DataLake",
                    sdxInternalTestDto.getAllInstanceIps(testContext), sdxInternalTestDto.getSelinuxMode(), generateDenyReports);
        }

        DistroXTestDto distroXTestDto = testContext.get(DistroXTestDto.class);
        if (distroXTestDto != null) {
            validateAll(validationBuilder, testContext, "DataHub",
                    distroXTestDto.getAllInstanceIps(testContext), distroXTestDto.getSelinuxMode(), generateDenyReports);
        }

        ValidationResult validationResult = validationBuilder.build();
        if (throwIfAnyError) {
            throwIfAnyError(validationResult);
        } else {
            LOGGER.error("The SELinux validation found the following errors: \n{}", validationResult.getFormattedErrors());
        }
    }

    public FreeIpaTestDto validateAll(TestContext testContext, FreeIpaTestDto testDto, boolean throwIfAnyError, boolean generateDenyReport) {
        ValidationResult validationResult = validateAll("FreeIPA", testContext,
                testDto.getAllInstanceIps(testContext), testDto.getSelinuxMode(), generateDenyReport).build();
        if (throwIfAnyError) {
            throwIfAnyError(validationResult);
        } else {
            LOGGER.error("The SELinux validation found the following errors on the FreeIPA instances: \n{}", validationResult.getFormattedErrors());
        }
        return testDto;
    }

    public SdxInternalTestDto validateAll(TestContext testContext, SdxInternalTestDto testDto, boolean throwIfAnyError, boolean generateDenyReport) {
        ValidationResult validationResult = validateAll("DataLake", testContext,
                testDto.getAllInstanceIps(testContext), testDto.getSelinuxMode(), generateDenyReport).build();
        if (throwIfAnyError) {
            throwIfAnyError(validationResult);
        } else {
            LOGGER.error("The SELinux validation found the following errors on the DataLake instances: \n{}", validationResult.getFormattedErrors());
        }
        return testDto;
    }

    public DistroXTestDto validateAll(TestContext testContext, DistroXTestDto testDto, boolean throwIfAnyError, boolean generateDenyReport) {
        ValidationResult validationResult = validateAll("DataHub", testContext,
                testDto.getAllInstanceIps(testContext), testDto.getSelinuxMode(), generateDenyReport).build();
        if (throwIfAnyError) {
            throwIfAnyError(validationResult);
        } else {
            LOGGER.error("The SELinux validation found the following errors on the DataHub instances: \n{}", validationResult.getFormattedErrors());
        }
        return testDto;
    }

    private ValidationResult.ValidationResultBuilder validateAll(String stackType, TestContext testContext, List<String> instanceIps,
            SeLinux expectedSelinuxMode, boolean generateDenyReports) {
        return validateAll(null, testContext, stackType, instanceIps, expectedSelinuxMode, generateDenyReports);
    }

    private ValidationResult.ValidationResultBuilder validateAll(ValidationResult.ValidationResultBuilder validationBuilder, TestContext testContext,
            String stackType, List<String> instanceIps, SeLinux expectedSelinuxMode, boolean generateDenyReports) {
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
            if (generateDenyReports) {
                generateReportFromDenies(testContext, stackType, instancesWithAuditedDenies);
            }
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

    private void generateReportFromDenies(TestContext testContext, String stackType, Map<String, String> instancesWithAuditedDenies) {
        if (!instancesWithAuditedDenies.isEmpty()) {
            String filename = REPORT_DIRECTORY + "/" + testContext.getTestMethodName().orElse("unknown") + "/" + stackType + ".json";
            Path path = Paths.get(filename);
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, instancesWithAuditedDenies.toString());
            } catch (IOException e) {
                LOGGER.warn("There was an unexpected error during saving the SELinux report to the file '{}'", filename, e);
            }
        } else {
            LOGGER.info("The SELinux validation found no audited denies on {} instances, so no report was generated.", stackType);
        }
    }

    private static void throwIfAnyError(ValidationResult validationResult) {
        if (validationResult.hasError()) {
            throw new TestFailException(validationResult.getFormattedErrors());
        }
    }
}
