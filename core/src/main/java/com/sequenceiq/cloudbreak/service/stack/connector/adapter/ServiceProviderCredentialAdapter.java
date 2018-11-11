package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static com.sequenceiq.cloudbreak.cloud.model.CloudCredential.SMART_SENSE_ID;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.credential.CredentialPrerequisiteService;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

import reactor.bus.EventBus;

@Component
public class ServiceProviderCredentialAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderCredentialAdapter.class);

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

    public Credential init(Credential credential, Long workspaceId, String userId) {
        credential = credentialPrerequisiteService.decorateCredential(credential, userId);
        CloudContext cloudContext = new CloudContext(credential.getId(), credential.getName(), credential.cloudPlatform(),
                credential.getOwner(), userId, workspaceId);
        CloudCredential cloudCredential = credentialConverter.convert(credential);

        CredentialVerificationRequest request = new CredentialVerificationRequest(cloudContext, cloudCredential);
        LOGGER.info("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            CredentialVerificationResult res = request.await();
            String message = "Failed to verify the credential: ";
            LOGGER.info("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.error(message, res.getErrorDetails());
                throw new BadRequestException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            if (CredentialStatus.FAILED.equals(res.getCloudCredentialStatus().getStatus())) {
                throw new BadRequestException(message + res.getCloudCredentialStatus().getStatusReason(),
                        res.getCloudCredentialStatus().getException());
            }
            CloudCredential cloudCredentialResponse = res.getCloudCredentialStatus().getCloudCredential();
            mergeSmartSenseAttributeIfExists(credential, cloudCredentialResponse);
            mergeCloudProviderParameters(credential, cloudCredentialResponse, Collections.singleton(SMART_SENSE_ID));
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing credential verification", e);
            throw new OperationException(e);
        }
        return credential;
    }

    public Map<String, String> interactiveLogin(Credential credential, Long workspaceId, String userId) {
        CloudContext cloudContext = new CloudContext(credential.getId(), credential.getName(),
                credential.cloudPlatform(), credential.getOwner(), userId, workspaceId);
        ExtendedCloudCredential cloudCredential = extendedCloudCredentialConverter.convert(credential);
        InteractiveLoginRequest request = new InteractiveLoginRequest(cloudContext, cloudCredential);
        LOGGER.info("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            InteractiveLoginResult res = request.await();
            String message = "Interactive login Failed: ";
            LOGGER.info("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.error(message, res.getErrorDetails());
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

    private void mergeSmartSenseAttributeIfExists(Credential credential, CloudCredential cloudCredentialResponse) {
        String smartSenseId = String.valueOf(cloudCredentialResponse.getParameters().get(SMART_SENSE_ID));
        if (StringUtils.isNoneEmpty(smartSenseId)) {
            try {
                Json attributes = new Json(credential.getAttributes());
                Map<String, Object> newAttributes = attributes.getMap();
                newAttributes.put(SMART_SENSE_ID, smartSenseId);
                credential.setAttributes(new Json(newAttributes).getValue());
            } catch (IOException e) {
                LOGGER.error("SmartSense id could not be added to the credential as attribute.", e);
            }
        }
    }

    private void mergeCloudProviderParameters(Credential credential, CloudCredential cloudCredentialResponse, Set<String> skippedKeys) {
        Json attributes = new Json(credential.getAttributes());
        Map<String, Object> newAttributes = attributes.getMap();
        boolean newAttributesAdded = false;
        for (Map.Entry<String, Object> cloudParam : cloudCredentialResponse.getParameters().entrySet()) {
            if (!skippedKeys.contains(cloudParam.getKey()) && newAttributes.get(cloudParam.getKey()) == null && cloudParam.getValue() != null) {
                newAttributes.put(cloudParam.getKey(), cloudParam.getValue());
                newAttributesAdded = true;
            }
        }
        if (newAttributesAdded) {
            try {
                credential.setAttributes(new Json(newAttributes).getValue());
            } catch (IOException ex) {
                LOGGER.error("New cloudprovider attributes could not be added to the credential.", ex);
                throw new OperationException(ex);
            }
        }
    }
}
