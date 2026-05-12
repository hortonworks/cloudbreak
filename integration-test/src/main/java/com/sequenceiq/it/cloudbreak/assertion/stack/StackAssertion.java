package com.sequenceiq.it.cloudbreak.assertion.stack;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@Component
public class StackAssertion {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackAssertion.class);

    private static final String VALIDATE_LOAD_BALANCER_CMD = "nslookup %s 2>&1 | tee ./nslookup_out | grep -q '%s' && " +
            "echo Success || echo Failure - $(hostname) - $(grep -v '%s' ./nslookup_out)";

    private static final String VALIDATE_FILE_CONTENT_CMD = "sudo grep -Pq '%s' %s && echo Success || echo Failure";

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

    private void validateFileContentExists(StackV4Response stackV4Response, String fileName, String fileContent) {
        try {
            String cmd = String.format(VALIDATE_FILE_CONTENT_CMD, fileContent, fileName);
            Map<String, Pair<Integer, String>> results = sshJClientActions.executeSshCommandOnPrimaryGateways(
                    stackV4Response.getInstanceGroups(), cmd, false);

            List<String> errors = results
                    .values()
                    .stream()
                    .map(Pair::getValue)
                    .filter(value -> value != null && value.contains("Failure"))
                    .toList();

            if (!errors.isEmpty()) {
                throw new RuntimeException("File content does not exist in " + fileName);
            }

        } catch (Exception e) {
            LOGGER.error("Error trying to check {} in {}", fileContent, fileName, e);
            throw new TestFailException("Error trying to check file content exists: " + e.getMessage(), e);
        }
    }
}

