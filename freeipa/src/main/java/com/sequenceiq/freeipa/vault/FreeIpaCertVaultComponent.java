package com.sequenceiq.freeipa.vault;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.entity.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class FreeIpaCertVaultComponent {

    private static final String CLIENT_CERTIFICATE_VAULT_PATH = "%s/gatewaycerts/%s/%s/clientcertificate";

    private static final String CLIENT_KEY_VAULT_PATH = "%s/gatewaycerts/%s/%s/clientkey";

    private static final String VAULT_UPDATE_FAILED = "Failed to update Vault.";

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCertVaultComponent.class);

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @Inject
    private SecretService secretService;

    private static String toClientCertificateVaultPath(Stack stack) {
        return String.format(CLIENT_CERTIFICATE_VAULT_PATH, stack.getAccountId(),
            Crn.safeFromString(stack.getEnvironmentCrn()).getResource(),
            Crn.safeFromString(stack.getResourceCrn()).getResource());
    }

    private static String toClientKeyVaultPath(Stack stack) {
        return String.format(CLIENT_KEY_VAULT_PATH, stack.getAccountId(),
            Crn.safeFromString(stack.getEnvironmentCrn()).getResource(),
            Crn.safeFromString(stack.getResourceCrn()).getResource());
    }

    private SecretResponse putSecret(String path, String value) {
        try {
            String secret = secretService.put(path, value);
            return stringToSecretResponseConverter.convert(secret);
        } catch (Exception exception) {
            LOGGER.warn("Failure while updating vault.", exception);
            throw new RuntimeException(VAULT_UPDATE_FAILED);
        }
    }

    public SecretResponse putGatewayClientCertificate(Stack stack, String clientCertificate) {
        String path = toClientCertificateVaultPath(stack);
        return putSecret(path, clientCertificate);
    }

    public SecretResponse putGatewayClientKey(Stack stack, String clientKey) {
        String path = toClientKeyVaultPath(stack);
        return putSecret(path, clientKey);
    }

    public void cleanupSecrets(Stack stack) {
        secretService.cleanup(toClientCertificateVaultPath(stack));
        secretService.cleanup(toClientKeyVaultPath(stack));
    }
}
