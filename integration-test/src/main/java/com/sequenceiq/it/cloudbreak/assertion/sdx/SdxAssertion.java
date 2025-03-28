package com.sequenceiq.it.cloudbreak.assertion.sdx;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@Component
public class SdxAssertion {

    private static final String VALIDATE_LOAD_BALANCER_CMD = "nslookup %s | grep -q '%s' && echo Success || echo Failure - $(hostname)";

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxAssertion.class);

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public void validateLoadBalancerFQDNInTheHosts(SdxTestDto sdxTestDto, List<LoadBalancerResponse> loadBalancers) {
        LoadBalancerResponse loadBalancerResponse =
                loadBalancers
                        .stream()
                        .filter(Predicate.not(lb -> lb.getType() == LoadBalancerType.OUTBOUND))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Loadbalancer not found"));

        try {
            String cmd = String.format(VALIDATE_LOAD_BALANCER_CMD, loadBalancerResponse.getFqdn(), loadBalancerResponse.getCloudDns());
            Map<String, Pair<Integer, String>> results = sshJClientActions.executeSshCommandOnAllHosts(
                    sdxTestDto.getResponse().getStackV4Response().getInstanceGroups(), cmd, false, commonCloudProperties.getDefaultPrivateKeyFile());

            List<String> errors = results
                    .values()
                    .stream()
                    .map(Pair::getValue)
                    .filter(value -> value != null && value.contains("Failure"))
                    .collect(toList());

            if (!errors.isEmpty()) {
                throw new RuntimeException("Loadbalancer is not resolvable. Error messages: " + errors);
            }
        } catch (Exception e) {
            LOGGER.error("Error trying to check load balancer FQDN", e);
            throw new TestFailException("Error trying to check load balancer FQDN: " + e.getMessage(), e);
        }
    }
}

