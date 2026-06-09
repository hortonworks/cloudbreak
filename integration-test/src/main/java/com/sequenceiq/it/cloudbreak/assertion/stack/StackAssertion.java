package com.sequenceiq.it.cloudbreak.assertion.stack;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@Component
public class StackAssertion {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackAssertion.class);

    private static final String VALIDATE_LOAD_BALANCER_CMD = "nslookup %s 2>&1 | tee ./nslookup_out | grep -q '%s' && " +
            "echo Success || echo Failure - $(hostname) - $(grep -v '%s' ./nslookup_out)";

    private static final String VALIDATE_FILE_CONTENT_CMD = "sudo grep -Pq '%s' %s && echo Success || echo Failure";

    private static final String VALIDATE_FILE_CONTENT_DOES_NOT_EXIST_CMD = "sudo grep -Pq '%s' %s && echo Failure || echo Success";

    private static final String VALIDATE_FILE_NOT_EXISTS_CMD = "sudo test ! -e %s && echo Success || echo Failure";

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public void validateLoadBalancerFQDNInTheHosts(SdxTestDto sdxTestDto, List<LoadBalancerResponse> loadBalancers) {
        validateLoadBalancerFQDNInTheHosts(sdxTestDto.getResponse().getStackV4Response(), loadBalancers);
    }

    public void validateLoadBalancerFQDNInTheHosts(DistroXTestDto distroXTestDto, List<LoadBalancerResponse> loadBalancers) {
        validateLoadBalancerFQDNInTheHosts(distroXTestDto.getResponses().stream().findFirst().get(), loadBalancers);
    }

    private void validateLoadBalancerFQDNInTheHosts(StackV4Response stackV4Response, List<LoadBalancerResponse> loadBalancers) {
        LoadBalancerResponse loadBalancerResponse =
                loadBalancers
                        .stream()
                        .filter(Predicate.not(lb -> lb.getType() == LoadBalancerType.OUTBOUND))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Loadbalancer not found"));

        try {
            String resolveTo = stackV4Response.getCloudPlatform().equals(CloudPlatform.AWS) ? loadBalancerResponse.getCloudDns() : loadBalancerResponse.getIp();
            String cmd = String.format(VALIDATE_LOAD_BALANCER_CMD, loadBalancerResponse.getFqdn(), resolveTo, resolveTo);
            Map<String, Pair<Integer, String>> results = sshJClientActions.executeSshCommandOnAllHosts(
                    stackV4Response.getInstanceGroups(), cmd, false, commonCloudProperties.getDefaultPrivateKeyFile());

            List<String> errors = results
                    .values()
                    .stream()
                    .map(Pair::getValue)
                    .filter(value -> value != null && value.contains("Failure"))
                    .toList();

            if (!errors.isEmpty()) {
                throw new RuntimeException(String.format("Loadbalancer FQDN %s is not resolvable to %s. Error messages: ", loadBalancerResponse.getCloudDns(),
                        resolveTo) + errors);
            }
        } catch (Exception e) {
            LOGGER.error("Error trying to check load balancer FQDN", e);
            throw new TestFailException("Error trying to check load balancer FQDN: " + e.getMessage(), e);
        }
    }

    public void validateFileContentExists(SdxTestDto sdxTestDto, String fileName, String fileContent) {
        validateFileContentExists(sdxTestDto.getResponse().getStackV4Response(), fileName, fileContent);
    }

    public void validateFileContentExists(DistroXTestDto distroXTestDto, String fileName, String fileContent) {
        validateFileContentExists(distroXTestDto.getResponse(), fileName, fileContent);
    }

    public void validateFileContentExists(FreeIpaTestDto freeIpaTestDto, String fileName, String fileContent) {
        Set<InstanceMetaDataResponse> instanceMetaDatas = freeIpaTestDto.getResponse().getInstanceGroups().stream()
                .map(InstanceGroupResponse::getMetaData)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        String cmd = String.format(VALIDATE_FILE_CONTENT_CMD, fileContent, fileName);
        assertSshResult(() -> sshJClientActions.executeSshCommandOnHost(instanceMetaDatas, cmd, false), cmd,
                () -> "Pattern '" + fileContent + "' not found in " + fileName,
                "Error trying to check file content exists on FreeIPA");
    }

    private void validateFileContentExists(StackV4Response stackV4Response, String fileName, String fileContent) {
        assertSshResultOnPrimaryGateways(stackV4Response, String.format(VALIDATE_FILE_CONTENT_CMD, fileContent, fileName),
                () -> "Pattern '" + fileContent + "' not found in " + fileName,
                "Error trying to check file content exists");
    }

    public void validateFileContentDoesNotExist(DistroXTestDto distroXTestDto, String fileName, String fileContent) {
        validateFileContentDoesNotExist(distroXTestDto.getResponse(), fileName, fileContent);
    }

    private void validateFileContentDoesNotExist(StackV4Response stackV4Response, String fileName, String fileContent) {
        assertSshResultOnPrimaryGateways(stackV4Response, String.format(VALIDATE_FILE_CONTENT_DOES_NOT_EXIST_CMD, fileContent, fileName),
                () -> "Pattern " + fileContent + " unexpectedly matches content of " + fileName,
                "Error trying to check file content is absent");
    }

    public void validateFileNotExists(DistroXTestDto distroXTestDto, String fileName) {
        validateFileNotExists(distroXTestDto.getResponse(), fileName);
    }

    private void validateFileNotExists(StackV4Response stackV4Response, String fileName) {
        assertSshResultOnPrimaryGateways(stackV4Response, String.format(VALIDATE_FILE_NOT_EXISTS_CMD, fileName),
                () -> "File " + fileName + " unexpectedly exists on one or more gateways",
                "Error trying to check file does not exist");
    }

    private void assertSshResultOnPrimaryGateways(StackV4Response stackV4Response, String cmd, Supplier<String> assertionErrorMessage,
            String testFailureContext) {
        assertSshResult(() -> sshJClientActions.executeSshCommandOnPrimaryGateways(stackV4Response.getInstanceGroups(), cmd, false),
                cmd, assertionErrorMessage, testFailureContext);
    }

    private void assertSshResult(Supplier<Map<String, Pair<Integer, String>>> sshExecutor, String cmd, Supplier<String> assertionErrorMessage,
            String testFailureContext) {
        try {
            Map<String, Pair<Integer, String>> results = sshExecutor.get();

            boolean anyFailure = results
                    .values()
                    .stream()
                    .map(Pair::getValue)
                    .anyMatch(value -> value != null && value.contains("Failure"));

            if (anyFailure) {
                throw new RuntimeException(assertionErrorMessage.get());
            }
        } catch (Exception e) {
            LOGGER.error("{} (cmd={})", testFailureContext, cmd, e);
            throw new TestFailException(testFailureContext + ": " + e.getMessage(), e);
        }
    }
}

