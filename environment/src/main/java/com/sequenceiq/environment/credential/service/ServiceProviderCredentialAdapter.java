package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.environment.TempConstants.TEMP_USER_ID;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CDPServicePolicyVerificationException;
import com.sequenceiq.cloudbreak.cloud.event.credential.CDPServicePolicyVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CDPServicePolicyVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowResponse;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponses;
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
import com.sequenceiq.environment.environment.verification.CDPServicePolicyVerification;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Component
public class ServiceProviderCredentialAdapter {

    public static final String FAILED_CREDETIAL_VERIFICATION_MESSAGE = "Failed to verify the credential "
            + "(try few minutes later if policies and roles are newly created or modified): ";

    public static final String FAILED_POLICY_VERIFICATION_MESSAGE = "Failed to verify the policy "
            + "(try few minutes later if policies and roles are newly created or modified): ";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderCredentialAdapter.class);

    private static final String FAILED_CREDENTIAL_VERFICIATION_MSG = "Couldn't verify credential.";

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
        return verify(credential, accountId, Boolean.FALSE);
    }

    public CredentialVerification verify(Credential credential, String accountId, boolean creationVerification) {
        boolean changed = false;
        credential = credentialPrerequisiteService.decorateCredential(credential);
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(credential.getId())
                .withName(credential.getName())
                .withCrn(credential.getResourceCrn())
                .withPlatform(credential.getCloudPlatform())
                .withVariant(credential.getCloudPlatform())
                .withAccountId(accountId)
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credential);

        CredentialVerificationRequest request = requestProvider.getCredentialVerificationRequest(cloudContext, cloudCredential, creationVerification);
        LOGGER.debug("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            CredentialVerificationResult res = request.await();
            String message = FAILED_CREDETIAL_VERIFICATION_MESSAGE;
            LOGGER.debug("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.info(message, res.getErrorDetails());
                throw new CredentialVerificationException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            CloudCredentialStatus cloudCredentialStatus = res.getCloudCredentialStatus();
            if (CredentialStatus.FAILED.equals(cloudCredentialStatus.getStatus())) {
                return new CredentialVerification(credential, setNewStatusText(credential, cloudCredentialStatus));
            }
            changed = setNewStatusText(credential, cloudCredentialStatus);
            CloudCredential cloudCredentialResponse = cloudCredentialStatus.getCloudCredential();
            if (cloudCredentialStatus.isDefaultRegionChanged()) {
                changed = mergeCloudProviderParameters(credential, cloudCredentialResponse, Collections.singleton(SMART_SENSE_ID));
            }
            changed = changed || mergeCloudProviderParameters(credential, cloudCredentialResponse, Collections.singleton(SMART_SENSE_ID));
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing credential verification", e);
            throw new OperationException(e);
        }
        return new CredentialVerification(credential, changed);
    }

    public CDPServicePolicyVerification verifyByServices(Credential credential, String accountId,
        List<String> services,
        Map<String, String> experiencePrerequisites) {
        credential = credentialPrerequisiteService.decorateCredential(credential);
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(credential.getId())
                .withName(credential.getName())
                .withCrn(credential.getResourceCrn())
                .withPlatform(credential.getCloudPlatform())
                .withVariant(credential.getCloudPlatform())
                .withUserName(TEMP_USER_ID)
                .withAccountId(accountId)
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credential);

        CDPServicePolicyVerificationRequest request = requestProvider.getCDPServicePolicyVerificationRequest(
                cloudContext,
                cloudCredential,
                services,
                experiencePrerequisites);
        LOGGER.debug("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            CDPServicePolicyVerificationResult res = request.await();
            LOGGER.debug("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                String message = FAILED_POLICY_VERIFICATION_MESSAGE;
                LOGGER.info(message, res.getErrorDetails());
                throw new CDPServicePolicyVerificationException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            CDPServicePolicyVerificationResponses cdpServicePolicyVerificationResponses = res.getCdpServicePolicyVerificationResponses();
            return new CDPServicePolicyVerification(cdpServicePolicyVerificationResponses.getResults());
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing credential verification", e);
            throw new OperationException(e);
        }
    }

    private boolean setNewStatusText(Credential credential, CloudCredentialStatus status) {
        boolean changed = false;
        String originalVerificationStatusText = credential.getVerificationStatusText();
        if (CredentialStatus.PERMISSIONS_MISSING.equals(status.getStatus())) {
            if (!StringUtils.equals(originalVerificationStatusText, status.getStatusReason())) {
                credential.setVerificationStatusText(status.getStatusReason());
                changed = true;
            }
        } else if (CredentialStatus.FAILED.equals(status.getStatus())) {
            String failedMessage = FAILED_CREDENTIAL_VERFICIATION_MSG + " Reason: " + status.getStatusReason();
            if (!Objects.equals(credential.getVerificationStatusText(), failedMessage)) {
                credential.setVerificationStatusText(failedMessage);
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

    public Map<String, String> interactiveLogin(Credential credential, String accountId) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(credential.getId())
                .withName(credential.getName())
                .withCrn(credential.getResourceCrn())
                .withPlatform(credential.getCloudPlatform())
                .withVariant(credential.getCloudPlatform())
                .withAccountId(accountId)
                .build();
        ExtendedCloudCredential cloudCredential = extendedCloudCredentialConverter.convert(credential);
        LOGGER.debug("Requesting interactive login cloudPlatform {} and creator {}.", credential.getCloudPlatform());
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

    public Credential initCodeGrantFlow(Credential credential, String accountId) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(credential.getId())
                .withName(credential.getName())
                .withCrn(credential.getResourceCrn())
                .withPlatform(credential.getCloudPlatform())
                .withVariant(credential.getCloudPlatform())
                .withAccountId(accountId)
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        LOGGER.debug("Requesting code grant flow cloudPlatform {} and creator {}.", credential.getCloudPlatform());
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
