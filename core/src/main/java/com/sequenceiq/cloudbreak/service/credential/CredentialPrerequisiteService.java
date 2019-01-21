package com.sequenceiq.cloudbreak.service.credential;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialPrerequisitesV4Response;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserPreferences;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.sharedservice.ServiceDescriptorDefinitionProvider;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.user.UserPreferencesService;

import reactor.bus.EventBus;

@Service
public class CredentialPrerequisiteService {

    public static final String CUMULUS_AMBARI_URL = "cumulusAmbariUrl";

    public static final String CUMULUS_AMBARI_USER = "cumulusAmbariUser";

    public static final String CUMULUS_AMBARI_PASSWORD = "cumulusAmbariPassword";

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialPrerequisiteService.class);

    private static final String ROLE_ARN_PARAMTER_KEY = "roleArn";

    private static final String EXTERNAL_ID_PARAMETER_KEY = "externalId";

    private static final String CUMULUS_YARN_ENDPOINT = "cumulusYarnEndpoint";

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private UserPreferencesService userPreferencesService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    public CredentialPrerequisitesV4Response getPrerequisites(User user, Workspace workspace, String cloudPlatform, String deploymentAddress) {
        CloudContext cloudContext = new CloudContext(null, null, cloudPlatform, user.getUserId(), workspace.getId());
        UserPreferences userPreferences = userPreferencesService.getWithExternalId(user);
        CredentialPrerequisitesRequest request = new CredentialPrerequisitesRequest(cloudContext, userPreferences.getExternalId(), deploymentAddress);
        LOGGER.debug("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        String message = String.format("Failed to get prerequisites for platform '%s': ", cloudPlatform);
        try {
            CredentialPrerequisitesResult res = request.await();
            LOGGER.debug("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.info(message, res.getErrorDetails());
                throw new BadRequestException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            return res.getCredentialPrerequisitesV4Response();
        } catch (InterruptedException e) {
            LOGGER.error(message, e);
            throw new OperationException(e);
        }
    }

    public Credential decorateCredential(Credential credential, String userId) {
        String attributes = credential.getAttributes();
        Map<String, Object> newAttributes = convertJsonStringToMap(attributes);
        boolean attributesChanged = false;
        if (StringUtils.isNoneEmpty((String) newAttributes.get(ROLE_ARN_PARAMTER_KEY))) {
            Optional<UserPreferences> userPreferencesOptional = userPreferencesService.getByUserId(userId);
            if (userPreferencesOptional.isPresent() && StringUtils.isNoneEmpty(userPreferencesOptional.get().getExternalId())) {
                String externalId = userPreferencesOptional.get().getExternalId();
                newAttributes.put(EXTERNAL_ID_PARAMETER_KEY, externalId);
                attributesChanged = true;
            }
        }
        if (isCumulusCredential(newAttributes)) {
            AmbariClient ambariClient = createCumulusAmbariClient(newAttributes);
            Map<String, String> params =
                    ambariClient.getConfigValuesByConfigIds(List.of(ServiceDescriptorDefinitionProvider.YARN_RESOURCEMANAGER_WEBAPP_ADDRESS));
            newAttributes.put(CUMULUS_YARN_ENDPOINT, "http://" + params.get(ServiceDescriptorDefinitionProvider.YARN_RESOURCEMANAGER_WEBAPP_ADDRESS));
            attributesChanged = true;
        }
        if (attributesChanged) {
            saveNewAttributesToCredential(credential, newAttributes);
        }
        return credential;
    }

    private Map<String, Object> convertJsonStringToMap(String attributes) {
        return isEmpty(attributes) ? new HashMap<>() : new Json(attributes).getMap();
    }

    public AmbariClient createCumulusAmbariClient(Map<String, Object> attributes) {
        if (isCumulusCredential(attributes)) {
            String datalakeAmbariUrl = (String) attributes.get(CredentialPrerequisiteService.CUMULUS_AMBARI_URL);
            try {
                URL ambariUrl = new URL(datalakeAmbariUrl);
                AmbariClient ambariClient = ambariClientProvider.getAmbariClient(ambariUrl, (String) attributes.get(CUMULUS_AMBARI_USER),
                        (String) attributes.get(CUMULUS_AMBARI_PASSWORD));
                return ambariClient;
            } catch (MalformedURLException e) {
                throw new CloudbreakServiceException("Datalake Ambari URL is malformed: " + datalakeAmbariUrl, e);
            }
        } else {
            throw new CloudbreakServiceException("Cannot create Ambari client from non Cumulus credential!");
        }
    }

    public AmbariClient createCumulusAmbariClient(String attributes) {
        return createCumulusAmbariClient(convertJsonStringToMap(attributes));
    }

    public boolean isCumulusCredential(Map<String, Object> attributes) {
        return StringUtils.isNoneEmpty((String) attributes.get(CUMULUS_AMBARI_URL));
    }

    public boolean isCumulusCredential(String attributes) {
        Map<String, Object> attributeMap = convertJsonStringToMap(attributes);
        return isCumulusCredential(attributeMap);
    }

    private void saveNewAttributesToCredential(Credential credential, Map<String, Object> newAttributes) {
        try {
            credential.setAttributes(new Json(newAttributes).getValue());
        } catch (IOException ex) {
            LOGGER.info("New prerequisite attributes could not be added to the credential.", ex);
            throw new OperationException(ex);
        }
    }
}
