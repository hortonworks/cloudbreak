package com.sequenceiq.cloudbreak.cloud.handler;

import static com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponse.SERVICE_UNAVAILABLE;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CDPServicePolicyVerificationException;
import com.sequenceiq.cloudbreak.cloud.event.credential.CDPServicePolicyVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CDPServicePolicyVerificationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponse;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponses;

import reactor.bus.Event;

@Component
public class CDPServicePolicyVerificationHandler implements CloudPlatformEventHandler<CDPServicePolicyVerificationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPServicePolicyVerificationHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<CDPServicePolicyVerificationRequest> type() {
        return CDPServicePolicyVerificationRequest.class;
    }

    @Override
    public void accept(Event<CDPServicePolicyVerificationRequest> cdpServicePolicyVerificationRequest) {
        LOGGER.debug("Received event: {}", cdpServicePolicyVerificationRequest);
        CDPServicePolicyVerificationRequest request = cdpServicePolicyVerificationRequest.getData();
        List<String> services = request.getServices();
        Map<String, String> experiencePrerequisites = request.getExperiencePrerequisites();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.getDefault(request.getCloudContext().getPlatform());
            AuthenticatedContext ac;
            CDPServicePolicyVerificationResponses cdpServicePolicyVerificationResponses;
            try {
                ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
                cdpServicePolicyVerificationResponses = connector.credentials().verifyByServices(ac, services, experiencePrerequisites);
            } catch (CDPServicePolicyVerificationException e) {
                String errorMessage = e.getMessage();
                LOGGER.info(errorMessage, e);
                cdpServicePolicyVerificationResponses = new CDPServicePolicyVerificationResponses(
                        getServiceStatusesMap(services, errorMessage));
            } catch (RuntimeException e) {
                String errorMessage = String.format("Could not verify credential [credential: '%s'], detailed message: %s",
                        request.getCloudContext().getName(), e.getMessage());
                LOGGER.warn(errorMessage, e);
                cdpServicePolicyVerificationResponses = new CDPServicePolicyVerificationResponses(
                        getServiceStatusesMap(services, errorMessage));
            }
            CDPServicePolicyVerificationResult credentialVerificationResult = new CDPServicePolicyVerificationResult(
                    request.getResourceId(),
                    cdpServicePolicyVerificationResponses);
            request.getResult().onNext(credentialVerificationResult);
            LOGGER.debug("Credential verification has finished");
        } catch (RuntimeException e) {
            request.getResult().onNext(new CDPServicePolicyVerificationResult(e.getMessage(), e, request.getResourceId()));
        }
    }

    private Set<CDPServicePolicyVerificationResponse> getServiceStatusesMap(List<String> services, String errorMessage) {
        Set<CDPServicePolicyVerificationResponse> cdpServicePolicyVerificationResponses = new HashSet<>();
        for (String service : services) {
            CDPServicePolicyVerificationResponse cdpServicePolicyVerificationResponse = new CDPServicePolicyVerificationResponse();
            cdpServicePolicyVerificationResponse.setStatusCode(SERVICE_UNAVAILABLE);
            cdpServicePolicyVerificationResponse.setServiceStatus(errorMessage);
            cdpServicePolicyVerificationResponse.setServiceName(service);
            cdpServicePolicyVerificationResponses.add(cdpServicePolicyVerificationResponse);
        }
        return cdpServicePolicyVerificationResponses;
    }

}
