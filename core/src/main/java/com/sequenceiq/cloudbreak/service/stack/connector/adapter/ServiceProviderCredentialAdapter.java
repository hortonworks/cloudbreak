package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static com.sequenceiq.cloudbreak.cloud.model.CloudCredential.SMART_SENSE_ID;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ServiceProviderCredentialAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderCredentialAdapter.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private OpenSshPublicKeyValidator rsaPublicKeyValidator;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    public Credential init(Credential credential) {
        if (!credential.passwordAuthenticationRequired()) {
            rsaPublicKeyValidator.validate(credential.getPublicKey());
        }
        CloudContext cloudContext = new CloudContext(credential.getId(), credential.getName(), credential.cloudPlatform(), credential.getOwner());
        CloudCredential cloudCredential = credentialConverter.convert(credential);

        CredentialVerificationRequest request = new CredentialVerificationRequest(cloudContext, cloudCredential);
        LOGGER.info("Triggering event: {}", request);
        eventBus.notify(request.selector(), Event.wrap(request));
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
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing credential verification", e);
            throw new OperationException(e);
        }
        return credential;
    }

    public Credential update(Credential credential) {
        return credential;
    }

    private void mergeSmartSenseAttributeIfExists(Credential credential, CloudCredential cloudCredentialResponse) {
        String smartSenseId = String.valueOf(cloudCredentialResponse.getParameters().get(SMART_SENSE_ID));
        if (StringUtils.isNoneEmpty(smartSenseId)) {
            try {
                Json attributes = credential.getAttributes();
                Map<String, Object> newAttributes = attributes.getMap();
                newAttributes.put(SMART_SENSE_ID, smartSenseId);
                credential.setAttributes(new Json(newAttributes));
            } catch (IOException e) {
                LOGGER.error("SmartSense id could not be added to the credential as attribute.", e);
            }
        }
    }
}
