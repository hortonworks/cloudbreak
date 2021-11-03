package com.sequenceiq.cloudbreak.service.existingstackfix;

import static com.sequenceiq.cloudbreak.domain.stack.StackFix.StackFixType.UNBOUND_RESTART;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackFix;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.cluster.GatewayConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;

@Service
public class UnboundRestartFixService extends ExistingStackFixService {

    static final String AFFECTED_STACK_VERSION = "7.2.11";

    static final Set<String> AFFECTED_IMAGE_IDS = Set.of(
            "26cd5a65-cd5c-457d-8d48-9caf1a486516",
            "19cf97b8-56d8-4be9-b317-998eea99d884",
            "c24acec3-9110-4474-9082-3620deac0910"
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(UnboundRestartFixService.class);

    @Inject
    private StackImageService stackImageService;

    @Inject
    private GatewayConfigProvider gatewayConfigProvider;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    public StackFix.StackFixType getStackFixType() {
        return UNBOUND_RESTART;
    }

    @Override
    public boolean isAffected(Stack stack) {
        if (!AFFECTED_STACK_VERSION.equals(stack.getStackVersion())) {
            return false;
        }
        try {
            Image image = stackImageService.getCurrentImage(stack);
            return AFFECTED_IMAGE_IDS.contains(image.getImageId());
        } catch (CloudbreakImageNotFoundException e) {
            throw new IllegalStateException("Image not found for stack " + stack.getResourceCrn(), e);
        }
    }

    @Override
    void doApply(Stack stack) {
        if (!isCmServerReachable(stack)) {
            LOGGER.info("UnboundRestartFixService cannot run, because CM server is unreachable of stack: {}", stack.getResourceCrn());
            throw new RuntimeException("CM server is unreachable for stack: " + stack.getResourceCrn());
        }
        try {
            Map<String, String> hostResponses = hostOrchestrator.replacePatternInFileOnAllHosts(gatewayConfigProvider.getGatewayConfig(stack),
                    "/etc/dhcp/dhclient-enter-hooks", "systemctl restart unbound", "pkill -u unbound -SIGHUP unbound");
            LOGGER.debug("Replace unbound service restart responses for stack {}: {}", stack.getResourceCrn(), hostResponses);
            List<String> failedHosts = hostResponses.entrySet().stream()
                    .filter(entry -> StringUtils.equalsAny(entry.getValue(), "false", "null", null))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (!failedHosts.isEmpty()) {
                throw new RuntimeException(String.format("Host(s) of stack %s had an invalid response: %s", stack.getResourceCrn(), failedHosts));
            }
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new RuntimeException("Failed to replace unbound service restart on all hosts", e);
        }
    }

    private boolean isCmServerReachable(Stack stack) {
        return stack.getInstanceGroups().stream()
                .flatMap(ig -> ig.getInstanceMetaDataSet().stream())
                .filter(InstanceMetaData::getClusterManagerServer)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find CM server for stack: " + stack.getResourceCrn()))
                .isReachable();
    }
}
