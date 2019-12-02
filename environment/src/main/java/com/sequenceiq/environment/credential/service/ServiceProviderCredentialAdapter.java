package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.environment.TempConstants.TEMP_USER_ID;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowResponse;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.exception.CredentialVerificationException;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.environment.credential.verification.CredentialVerification;
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

    public CredentialVerification verify(Credential credential, String accountId) {
        boolean changed = false;
        credential = credentialPrerequisiteService.decorateCredential(credential);
        CloudContext cloudContext =
                new CloudContext(credential.getId(), credential.getName(), credential.getCloudPlatform(), TEMP_USER_ID, accountId);
        CloudCredential cloudCredential = credentialConverter.convert(credential);

        CredentialVerificationRequest request = requestProvider.getCredentialVerificationRequest(cloudContext, cloudCredential);
        LOGGER.debug("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            CredentialVerificationResult res = request.await();
            String message = "Failed to verify the credential: ";
            LOGGER.debug("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.info(message, res.getErrorDetails());
                throw new CredentialVerificationException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            if (CredentialStatus.FAILED == res.getCloudCredentialStatus().getStatus()) {
                throw new CredentialVerificationException(message + res.getCloudCredentialStatus().getStatusReason(),
                        res.getCloudCredentialStatus().getException());
            }
            changed = setNewStatusText(credential, res.getCloudCredentialStatus());
            CloudCredential cloudCredentialResponse = res.getCloudCredentialStatus().getCloudCredential();
            changed = changed || mergeCloudProviderParameters(credential, cloudCredentialResponse, Collections.singleton(SMART_SENSE_ID));
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing credential verification", e);
            throw new OperationException(e);
        }
        return new CredentialVerification(credential, changed);
    }

    private boolean setNewStatusText(Credential credential, CloudCredentialStatus status) {
        boolean changed = false;
        String originalVerificationStatusText = credential.getVerificationStatusText();
        if (CredentialStatus.PERMISSIONS_MISSING == status.getStatus()) {
            if (!StringUtils.equals(originalVerificationStatusText, status.getStatusReason())) {
                credential.setVerificationStatusText(status.getStatusReason());
                changed = true;
            }
        } else {
            credential.setVerificationStatusText(null);
            if (originalVerificationStatusText != null) {
                changed = true;
            }
        }
        return changed;
    }

    public Map<String, String> interactiveLogin(Credential credential, String accountId, String userId) {
        CloudContext cloudContext = new CloudContext(credential.getId(), credential.getName(),
                credential.getCloudPlatform(), userId, accountId);
        ExtendedCloudCredential cloudCredential = extendedCloudCredentialConverter.convert(credential);
        LOGGER.debug("Requesting interactive login cloudPlatform {} and creator {}.", credential.getCloudPlatform(), userId);
        InteractiveLoginRequest request = requestProvider.getInteractiveLoginRequest(cloudContext, cloudCredential);
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
        LOGGER.debug("Requesting code grant flow cloudPlatform {} and creator {}.", credential.getCloudPlatform(), userId);
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
            addCodeGrantFlowInitAttributesToCredential(credential, codeGrantFlowInitParams);
            return credential;
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing initialization of authorization code grant based credential creation:", e);
            throw new OperationException(e);
        }
    }

    private boolean mergeCloudProviderParameters(Credential credential, CloudCredential cloudCredentialResponse, Set<String> skippedKeys) {
        return mergeCloudProviderParameters(credential, cloudCredentialResponse, skippedKeys, true);
    }

    private boolean mergeCloudProviderParameters(Credential credential, CloudCredential cloudCredentialResponse, Set<String> skippedKeys,
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
        return newAttributesAdded;
    }

    private void addCodeGrantFlowInitAttributesToCredential(Credential credential, Map<String, String> codeGrantFlowInitParams) {
        Json attributes = new Json(credential.getAttributes());
        Map<String, Object> newAttributes = attributes.getMap();
        Map<String, Object> azureAttributes = (Map<String, Object>) newAttributes.get(AzureCredentialView.PROVIDER_KEY);
        Map<String, String> codeGrantFlowAttributes = (Map<String, String>) azureAttributes.get(AzureCredentialView.CODE_GRANT_FLOW_BASED);
        codeGrantFlowAttributes.putAll(codeGrantFlowInitParams);
        credential.setAttributes(new Json(newAttributes).getValue());
    }
}
