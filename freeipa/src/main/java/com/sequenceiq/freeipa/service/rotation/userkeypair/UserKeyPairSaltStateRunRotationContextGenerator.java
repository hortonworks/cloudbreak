package com.sequenceiq.freeipa.service.rotation.userkeypair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyDescribeRequest;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.rotation.ExitCriteriaProvider;
import com.sequenceiq.freeipa.service.rotation.context.SaltRunOrchestratorStateRotationContext;

@Component
public class UserKeyPairSaltStateRunRotationContextGenerator {

    private static final int MAX_RETRY = 10;

    private static final String REPLACE_SSH_PUBLICKEY_STATE = "ssh.replace_ssh_publickey";

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    public SaltRunOrchestratorStateRotationContext generate(boolean changedKeyPair, String resourceCrn, Stack stack,
            DetailedEnvironmentResponse environment) {
        SaltRunOrchestratorStateRotationContext.SaltRunOrchestratorStateRotationContextBuilder saltRunOrchestratorStateRotationContextBuilder =
                new SaltRunOrchestratorStateRotationContext.SaltRunOrchestratorStateRotationContextBuilder();
        saltRunOrchestratorStateRotationContextBuilder.withResourceCrn(resourceCrn);
        if (!changedKeyPair) {
            saltRunOrchestratorStateRotationContextBuilder.noStateRunNeeded();
        } else {
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            Pair<String, String> publicKeys = getPublicKeys(environment, stack, changedKeyPair);
            saltRunOrchestratorStateRotationContextBuilder.withGatewayConfig(primaryGatewayConfig)
                    .withTargets(stack.getNotTerminatedInstanceMetaDataSet().stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toSet()))
                    .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                    .withMaxRetry(MAX_RETRY)
                    .withMaxRetryOnError(MAX_RETRY)
                    .withStates(List.of(REPLACE_SSH_PUBLICKEY_STATE))
                    .withRotateParams(getUserKeyPairRotationParams(environment.getAuthentication().getLoginUserName(), publicKeys.getRight()))
                    .withRollbackStates(List.of(REPLACE_SSH_PUBLICKEY_STATE))
                    .withRollbackParams(getUserKeyPairRotationParams(stack.getStackAuthentication().getLoginUserName(), publicKeys.getLeft()));
        }
        return saltRunOrchestratorStateRotationContextBuilder.build();
    }

    private String getPublicKeyFromProviderIfNotAvailable(String publicKeyId, String cloudPlatform, String region,
            String environmentCrn, String publicKey) {
        String result = publicKey;
        if (StringUtils.isEmpty(result)) {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
            PublicKeyConnector publicKeyConnector = Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).publicKey())
                    .orElseThrow(() -> new BadRequestException("No public key connector for cloud platform: " + cloudPlatform));
            PublicKeyDescribeRequest request = PublicKeyDescribeRequest.builder()
                    .withCredential(getCloudCredential(environmentCrn))
                    .withPublicKeyId(publicKeyId)
                    .withRegion(region)
                    .withCloudPlatform(cloudPlatform)
                    .build();
            result = publicKeyConnector.rawPublicKey(request);
        }
        return result;
    }

    private Map<String, Object> getUserKeyPairRotationParams(String username, String publicKey) {
        Map<String, String> sshParams = new HashMap<>();
        sshParams.put("user", username);
        sshParams.put("publickey", publicKey);
        return Map.of("userssh", sshParams);
    }

    private Pair<String, String> getPublicKeys(DetailedEnvironmentResponse environment, Stack stack, boolean changedKeyPair) {
        String publicKey = null;
        String oldPublicKey = null;
        if (changedKeyPair) {
            publicKey = getPublicKeyFromProviderIfNotAvailable(environment.getAuthentication().getPublicKeyId(),
                    environment.getCloudPlatform(),
                    stack.getRegion(),
                    environment.getCrn(),
                    environment.getAuthentication().getPublicKey());
            oldPublicKey = getPublicKeyFromProviderIfNotAvailable(stack.getStackAuthentication().getPublicKeyId(),
                    environment.getCloudPlatform(),
                    stack.getRegion(),
                    environment.getCrn(),
                    stack.getStackAuthentication().getPublicKey());
        }
        return Pair.of(oldPublicKey, publicKey);
    }

    private CloudCredential getCloudCredential(String environmentCrn) {
        Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
        return credentialConverter.convert(credential);
    }
}
