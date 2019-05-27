package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.environment.TempConstants.TEMP_USER_ID;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowResponse;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Component
public class ServiceProviderCredentialAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderCredentialAdapter.class);

    private static final String SMART_SENSE_ID = "smartSenseId";

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @Inject
    private RequestProvider requestProvider;

    public Credential verify(Credential credential, String accountId) {
        credential = credentialPrerequisiteService.decorateCredential(credential);
        CloudContext cloudContext =
                new CloudContext(credential.getId(), credential.getName(), credential.getCloudPlatform(), TEMP_USER_ID, accountId);
        CloudCredential cloudCredential = credentialConverter.convert(credential);

        CredentialVerificationRequest request = new CredentialVerificationRequest(cloudContext, cloudCredential);
        LOGGER.debug("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            CredentialVerificationResult res = request.await();
            String message = "Failed to verify the credential: ";
            LOGGER.debug("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.info(message, res.getErrorDetails());
                throw new BadRequestException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            if (CredentialStatus.FAILED.equals(res.getCloudCredentialStatus().getStatus())) {
                throw new BadRequestException(message + res.getCloudCredentialStatus().getStatusReason(),
                        res.getCloudCredentialStatus().getException());
            }
            CloudCredential cloudCredentialResponse = res.getCloudCredentialStatus().getCloudCredential();
            mergeCloudProviderParameters(credential, cloudCredentialResponse, Collections.singleton(SMART_SENSE_ID));
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing credential verification", e);
            throw new OperationException(e);
        }
        return credential;
    }

    public Map<String, String> interactiveLogin(Credential credential, String accountId, String userId) {
        CloudContext cloudContext = new CloudContext(credential.getId(), credential.getName(),
                credential.getCloudPlatform(), userId, accountId);
        ExtendedCloudCredential cloudCredential = extendedCloudCredentialConverter.convert(credential);
        InteractiveLoginRequest request = new InteractiveLoginRequest(cloudContext, cloudCredential);
        LOGGER.debug("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            InteractiveLoginResult res = request.await();
            String message = "Interactive login Failed: ";
            LOGGER.debug("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.info(message, res.getErrorDetails());
                throw new BadRequestException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            return res.getParameters();
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing credential verification", e);
            throw new OperationException(e);
        }
    }

    public Credential update(Credential credential) {
        return credential;
    }

    public Credential initCodeGrantFlow(Credential credential, String accountId, String userId) {
        CloudContext cloudContext = new CloudContext(credential.getId(), credential.getName(),
                credential.getCloudPlatform(), userId, accountId);
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        InitCodeGrantFlowRequest request = requestProvider.getInitCodeGrantFlowRequest(cloudContext, cloudCredential);
        LOGGER.info("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            InitCodeGrantFlowResponse res = request.await();
            LOGGER.info("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                String message = "Authorization code grant based credential creation couldn't be initialized: ";
                LOGGER.error(message, res.getErrorDetails());
                throw new BadRequestException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            Map<String, String> codeGrantFlowInitParams = res.getCodeGrantFlowInitParams();
            codeGrantFlowInitParams.forEach(cloudCredential::putParameter);
            mergeCloudProviderParameters(credential, cloudCredential, Collections.singleton(SMART_SENSE_ID));
            return credential;
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing initialization of authorization code grant based credential creation:", e);
            throw new OperationException(e);
        }
    }

    private void mergeCloudProviderParameters(Credential credential, CloudCredential cloudCredentialResponse, Set<String> skippedKeys) {
        mergeCloudProviderParameters(credential, cloudCredentialResponse, skippedKeys, true);
    }

    private void mergeCloudProviderParameters(Credential credential, CloudCredential cloudCredentialResponse, Set<String> skippedKeys,
            boolean overrideParameters) {
        Json attributes = new Json(credential.getAttributes());
        Map<String, Object> newAttributes = attributes.getMap();
        boolean newAttributesAdded = false;
        for (Entry<String, Object> cloudParam : cloudCredentialResponse.getParameters().entrySet()) {
            if (!skippedKeys.contains(cloudParam.getKey()) && cloudParam.getValue() != null) {
                if (overrideParameters || newAttributes.get(cloudParam.getKey()) == null) {
                    newAttributes.put(cloudParam.getKey(), cloudParam.getValue());
                    newAttributesAdded = true;
                }
            }
        }
        if (newAttributesAdded) {
            credential.setAttributes(new Json(newAttributes).getValue());
        }
    }
}
