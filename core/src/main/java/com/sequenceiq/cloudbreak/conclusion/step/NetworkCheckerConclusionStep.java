package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.GATEWAY_NETWORK_STATUS_FAILED;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CCM_NOT_ACCESSIBLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CCM_NOT_ACCESSIBLE_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_NEIGHBOUR_NOT_ACCESSIBLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_NEIGHBOUR_NOT_ACCESSIBLE_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_NGINX_UNREACHABLE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.node.status.CdpDoctorService;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorCheckStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorNetworkStatusResponse;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class NetworkCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkCheckerConclusionStep.class);

    private static final String NGINX_UNREACHABLE_ERROR_MESSAGE = "nginx is unreachable";

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private StackService stackService;

    @Inject
    private CdpDoctorService cdpDoctorService;

    @Override
    public Conclusion check(Long resourceId) {
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(resourceId);
            Map<String, CdpDoctorNetworkStatusResponse> resultForMinions = cdpDoctorService.getNetworkStatusForMinions(stack);
            List<String> networkFailures = new ArrayList<>();
            List<String> networkFailureDetails = new ArrayList<>();
            checkNetworkFailures(resultForMinions, networkFailures, networkFailureDetails);

            if (networkFailures.isEmpty()) {
                return succeeded();
            } else {
                return failed(networkFailures.toString(), networkFailureDetails.toString());
            }
        } catch (Exception e) {
            LOGGER.warn("Network report failed, error: {}", e.getMessage());
            if (e.getMessage().contains(NGINX_UNREACHABLE_ERROR_MESSAGE)) {
                String conclusion = cloudbreakMessagesService.getMessage(NETWORK_NGINX_UNREACHABLE);
                LOGGER.warn(conclusion);
                return failed(conclusion, e.getMessage());
            }
            return failed(cloudbreakMessagesService.getMessage(GATEWAY_NETWORK_STATUS_FAILED, List.of(e.getMessage())), e.getMessage());
        }
    }

    private void checkNetworkFailures(Map<String, CdpDoctorNetworkStatusResponse> resultForMinions,
            List<String> networkFailures, List<String> networkFailureDetails) {
        for (Map.Entry<String, CdpDoctorNetworkStatusResponse> entryForMinion : resultForMinions.entrySet()) {
            CdpDoctorNetworkStatusResponse networkResponseForMinion = entryForMinion.getValue();
            String host = entryForMinion.getKey();
            LOGGER.debug("Check network report for host: {}, details: {}", host, networkResponseForMinion);
            if (networkResponseForMinion.getCcmEnabled() && CdpDoctorCheckStatus.NOK.equals(networkResponseForMinion.getCcmAccessible())) {
                networkFailures.add(cloudbreakMessagesService.getMessageWithArgs(NETWORK_CCM_NOT_ACCESSIBLE, host));
                String details = cloudbreakMessagesService.getMessageWithArgs(NETWORK_CCM_NOT_ACCESSIBLE_DETAILS,
                        networkResponseForMinion.getCcmAccessible(), host);
                networkFailureDetails.add(details);
                LOGGER.warn(details);
            }
            if (CdpDoctorCheckStatus.NOK.equals(networkResponseForMinion.getClouderaComAccessible())) {
                networkFailures.add(cloudbreakMessagesService.getMessageWithArgs(NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE, host));
                String details = cloudbreakMessagesService.getMessageWithArgs(NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE_DETAILS,
                        networkResponseForMinion.getClouderaComAccessible(), host);
                networkFailureDetails.add(details);
                LOGGER.warn(details);
            }
            if (networkResponseForMinion.getNeighbourScan() && CdpDoctorCheckStatus.NOK.equals(networkResponseForMinion.getAnyNeighboursAccessible())) {
                networkFailures.add(cloudbreakMessagesService.getMessageWithArgs(NETWORK_NEIGHBOUR_NOT_ACCESSIBLE, host));
                String details = cloudbreakMessagesService.getMessageWithArgs(NETWORK_NEIGHBOUR_NOT_ACCESSIBLE_DETAILS,
                        networkResponseForMinion.getAnyNeighboursAccessible(), host);
                networkFailureDetails.add(details);
                LOGGER.warn(details);
            }
        }
    }
}
