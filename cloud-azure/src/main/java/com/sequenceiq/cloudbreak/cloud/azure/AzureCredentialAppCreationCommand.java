package com.sequenceiq.cloudbreak.cloud.azure;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.gs.collections.impl.bimap.mutable.HashBiMap;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service
public class AzureCredentialAppCreationCommand {

    private static final String DEFAULT_DEPLOYMENT_ADDRESS = "THE_ADDRESS_OF_YOUR_DEPLOYMENT";

    private static final long YEARS_OF_EXPIRATION = 3L;

    private static final String CB_AZ_APP_REDIRECT_URI_PATTERN = "delegatedtoken/v4/%s/credentials/code_grant_flow/authorization/azure";

    private static final String GENERATE_EXCEPTION_MESSAGE_FORMAT = "Failed to process the Azure AD App creation template from path: '%s'";

    private static final String ENCODING = "UTF-8";

    private static final String CB_AZ_APP_REPLY_URI = "delegatedtoken/v4/*";

    private static final String DELIMITER = "/";

    @Value("${cb.arm.app.creation.template.command.path:}")
    private String appCreationCommandTemplatePath;

    @Value("${cb.arm.app.creation.template.json.path:}")
    private String appCreationJSONTemplatePath;

    @Value("${cb.arm.app.creation.template.resource.app.id}")
    private String resourceAppId;

    @Value("${cb.arm.app.creation.template.resource.access.scope.id}")
    private String resourceAccessScopeId;

    @Inject
    private Configuration freemarkerConfiguration;

    public String generate(String deploymentAddress) {
        try {
            if (StringUtils.isEmpty(deploymentAddress)) {
                deploymentAddress = DEFAULT_DEPLOYMENT_ADDRESS;
            }
            Map<String, Object> model = buildModel(deploymentAddress, getAppIdentifierURI(deploymentAddress), getReplyURL(deploymentAddress));

            Template template = freemarkerConfiguration.getTemplate(appCreationCommandTemplatePath, ENCODING);
            return processTemplateIntoString(template, model);
        } catch (IOException | TemplateException e) {
            String message = String.format(GENERATE_EXCEPTION_MESSAGE_FORMAT, appCreationCommandTemplatePath);
            throw new CloudConnectorException(message, e);
        }
    }

    public AzureApplicationCreationView generateJSON(String deploymentAddress) {
        try {
            String appIdentifierURI = getAppIdentifierURI(deploymentAddress);
            String appSecret = UUID.randomUUID().toString();
            String replyURL = getReplyURL(deploymentAddress);
            Template template = freemarkerConfiguration.getTemplate(appCreationJSONTemplatePath, ENCODING);
            Map<String, Object> model = buildModel(deploymentAddress, appIdentifierURI, replyURL);
            model.put("appSecret", appSecret);
            model.put("keyId", UUID.randomUUID().toString());

            String creationRequestPayload = processTemplateIntoString(template, model);
            return new AzureApplicationCreationView(appIdentifierURI, appSecret, replyURL, creationRequestPayload);
        } catch (IOException | TemplateException e) {
            String message = String.format(GENERATE_EXCEPTION_MESSAGE_FORMAT, appCreationJSONTemplatePath);
            throw new CloudConnectorException(message, e);
        }
    }

    String getRedirectURL(String workspaceId, String deploymentAddress) {
        String cbAzAppAuthUri = String.format(CB_AZ_APP_REDIRECT_URI_PATTERN, workspaceId);
        String replyUrl = deploymentAddress.endsWith(DELIMITER) ? deploymentAddress : deploymentAddress + DELIMITER;
        return replyUrl + cbAzAppAuthUri;
    }

    private Map<String, Object> buildModel(String deploymentAddress, String appIdentifierURI, String replyURL) {
        Map<String, Object> model = new HashBiMap<>();
        model.put("cloudbreakAddress", deploymentAddress);
        model.put("identifierURI", appIdentifierURI);
        model.put("cloudbreakReplyUrl", replyURL);
        model.put("expirationDate", getExpirationDate());
        model.put("resourceAppId", resourceAppId);
        model.put("resourceAccessScopeId", resourceAccessScopeId);
        return model;
    }

    private String getAppIdentifierURI(String deploymentAddress) {
        String result = deploymentAddress.endsWith(DELIMITER) ? deploymentAddress : deploymentAddress + DELIMITER;
        return result + UUID.randomUUID().toString();
    }

    private String getReplyURL(String deploymentAddress) {
        String replyUrl = deploymentAddress.endsWith(DELIMITER) ? deploymentAddress : deploymentAddress + DELIMITER;
        return replyUrl + CB_AZ_APP_REPLY_URI;
    }

    private String getExpirationDate() {
        LocalDate date = LocalDate.now().plusYears(YEARS_OF_EXPIRATION);
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
