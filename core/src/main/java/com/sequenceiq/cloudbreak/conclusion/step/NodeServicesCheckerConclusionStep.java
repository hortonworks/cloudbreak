package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.GATEWAY_SERVICES_STATUS_FAILED;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SERVICES_CHECK_FAILED;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SERVICES_CHECK_FAILED_DETAILS;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.node.status.CdpDoctorService;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorCheckStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorServiceStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorServicesStatusResponse;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class NodeServicesCheckerConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeServicesCheckerConclusionStep.class);

    @Inject
    private StackService stackService;

    @Inject
    private CdpDoctorService cdpDoctorService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Override
    public Conclusion check(Long resourceId) {
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(resourceId);
            Map<String, CdpDoctorServicesStatusResponse> servicesStatusForMinions = cdpDoctorService.getServicesStatusForMinions(stack);
            Multimap<String, String> nodesWithUnhealthyServices = collectNodesWithUnhealthyServices(servicesStatusForMinions);
            if (nodesWithUnhealthyServices.isEmpty()) {
                return succeeded();
            } else {
                String conclusion = cloudbreakMessagesService.getMessageWithArgs(SERVICES_CHECK_FAILED, nodesWithUnhealthyServices.keySet());
                String details = cloudbreakMessagesService.getMessageWithArgs(SERVICES_CHECK_FAILED_DETAILS, nodesWithUnhealthyServices);
                LOGGER.warn(details);
                return failed(conclusion, details);
            }
        } catch (Exception e) {
            LOGGER.warn("Node services report failed, error: {}", e.getMessage());
            return failed(cloudbreakMessagesService.getMessage(GATEWAY_SERVICES_STATUS_FAILED), e.getMessage());
        }

    }

    private Multimap<String, String> collectNodesWithUnhealthyServices(Map<String, CdpDoctorServicesStatusResponse> servicesStatusForMinions) {
        Multimap<String, String> nodesWithUnhealthyServices = HashMultimap.create();
        for (Map.Entry<String, CdpDoctorServicesStatusResponse> resultEntry : servicesStatusForMinions.entrySet()) {
            String host = resultEntry.getKey();
            CdpDoctorServicesStatusResponse servicesStatusResponse = resultEntry.getValue();
            LOGGER.debug("Check node services report for host: {}", host);
            for (CdpDoctorServiceStatus serviceStatus : CollectionUtils.emptyIfNull(servicesStatusResponse.getInfraServices())) {
                if (CdpDoctorCheckStatus.NOK.equals(serviceStatus.getStatus())) {
                    nodesWithUnhealthyServices.put(host, serviceStatus.getName());
                }
            }
            for (CdpDoctorServiceStatus serviceStatus : CollectionUtils.emptyIfNull(servicesStatusResponse.getCmServices())) {
                if (CdpDoctorCheckStatus.NOK.equals(serviceStatus.getStatus())) {
                    nodesWithUnhealthyServices.put(host, serviceStatus.getName());
                }
            }
        }
        return nodesWithUnhealthyServices;
    }
}
