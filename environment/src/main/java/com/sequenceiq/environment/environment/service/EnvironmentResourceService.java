package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyDescribeRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.environment.api.v1.environment.model.request.CredentialAwareEnvRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;

@Service
public class EnvironmentResourceService {

    private static final int MAX_LENGTH_OF_PUBLIC_KEY_NAME = 255;

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentResourceService.class);

    private final CredentialService credentialService;

    private final NetworkService networkService;

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final Clock clock;

    private final Set<String> supportedExistingSshKeyUpdateProviders;

    private final Set<String> supportedRawSshKeyUpdateProviders;

    private ProxyConfigService proxyConfigService;

    private EncryptionProfileService encryptionProfileService;

    public EnvironmentResourceService(CredentialService credentialService, NetworkService networkService, CloudPlatformConnectors cloudPlatformConnectors,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter, Clock clock, ProxyConfigService proxyConfigService,
            @Value("${environment.existing.ssh.key.update.support:}") Set<String> supportedExistingSshKeyUpdateProviders,
            @Value("${environment.raw.ssh.key.update.support:}") Set<String> supportedRawSshKeyUpdateProviders,
            EncryptionProfileService encryptionProfileService) {
        this.credentialService = credentialService;
        this.networkService = networkService;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.clock = clock;
        this.proxyConfigService = proxyConfigService;
        this.supportedExistingSshKeyUpdateProviders = supportedExistingSshKeyUpdateProviders;
        this.supportedRawSshKeyUpdateProviders = supportedRawSshKeyUpdateProviders;
        this.encryptionProfileService = encryptionProfileService;
    }

    public Credential getCredentialFromRequest(CredentialAwareEnvRequest request, String accountId) {
        Credential credential;
        if (StringUtils.isNotEmpty(request.getCredentialName())) {
            try {
                credential = credentialService.getByNameForAccountId(request.getCredentialName(), accountId, ENVIRONMENT);
            } catch (NotFoundException e) {
                throw new BadRequestException(String.format("No credential found with name [%s] in the workspace.",
                        request.getCredentialName()), e);
            }
        } else {
            throw new BadRequestException("No credential has been specified in request for environment creation.");
        }
        return credential;
    }

    public Optional<ProxyConfig> getProxyConfig(String proxyConfigName, String accountId) {
        ProxyConfig proxyConfig = null;
        if (StringUtils.isNotEmpty(proxyConfigName)) {
            try {
                proxyConfig = proxyConfigService.getByNameForAccountId(proxyConfigName, accountId);
            } catch (NotFoundException e) {
                throw new BadRequestException(String.format("No ProxyConfig found with name [%s] in the workspace.", proxyConfigName), e);
            }
        }
        return Optional.ofNullable(proxyConfig);
    }

    public boolean createAndUpdateSshKey(Environment environment) {
        LOGGER.debug("Environment {} requested managed public key. Creating.", environment.getName());
        String publicKeyId = String.format("%s-%s", environment.getName(), environment.getResourceCrn());

        if (publicKeyId.length() <= MAX_LENGTH_OF_PUBLIC_KEY_NAME) {
            LOGGER.debug("Append timestamp to public key id ({}).", publicKeyId);
            publicKeyId += "-" + clock.getCurrentTimeMillis();
        } else {
            LOGGER.debug("The public key id  ({}) is longer than {}.", publicKeyId, MAX_LENGTH_OF_PUBLIC_KEY_NAME);
        }

        if (publicKeyId.length() > MAX_LENGTH_OF_PUBLIC_KEY_NAME) {
            String substring = publicKeyId.substring(0, MAX_LENGTH_OF_PUBLIC_KEY_NAME);
            LOGGER.debug("The public key id ({}) is chopped to {}.", publicKeyId, substring);
            publicKeyId = substring;
        }
        boolean created = createPublicKey(environment, publicKeyId);
        if (created) {
            environment.getAuthentication().setPublicKeyId(publicKeyId);
            LOGGER.debug("The public key id ({}) is updated in {}.", publicKeyId, environment.getName());
        }
        return created;
    }

    private boolean createPublicKey(Environment environment, String publicKeyId) {
        try {
            PublicKeyConnector publicKeyConnector = getPublicKeyConnector(environment.getCloudPlatform())
                    .orElseThrow(() -> new BadRequestException("No network connector for cloud platform: " + environment.getCloudPlatform()));
            PublicKeyRegisterRequest request = createPublicKeyRegisterRequest(environment, publicKeyId);
            publicKeyConnector.register(request);
            LOGGER.info("Public key id is registered with name of {}", publicKeyId);
            return true;
        } catch (UnsupportedOperationException e) {
            LOGGER.info("Cloud platform {} does not support public key services", environment.getCloudPlatform());
        } catch (Exception e) {
            LOGGER.info("Public key could not be registered. {}", e.getMessage(), e);
        }
        return false;
    }

    public Optional<PublicKeyConnector> getPublicKeyConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        Optional<PublicKeyConnector> publicKeyConnector = Optional.empty();
        try {
            publicKeyConnector = Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).publicKey());
        } catch (UnsupportedOperationException ignored) {
            LOGGER.info("Cloud platform {} does not support public key services", cloudPlatform);
        }
        return publicKeyConnector;
    }

    private PublicKeyRegisterRequest createPublicKeyRegisterRequest(Environment environment, String publicKeyId) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        return PublicKeyRegisterRequest.builder()
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(cloudCredential)
                .withPublicKeyId(publicKeyId)
                .withPublicKey(environment.getAuthentication().getPublicKey())
                .withRegion(environment.getLocation())
                .build();
    }

    public BaseNetwork createAndSetNetwork(Environment environment, NetworkDto networkDto, String accountId, Map<String, CloudSubnet> subnetMetas,
            Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        BaseNetwork network = networkService.saveNetwork(environment, networkDto, accountId, subnetMetas, endpointGatewaySubnetMetas);
        if (network != null) {
            environment.setNetwork(network);
        }
        return network;
    }

    public void deletePublicKey(Environment environment) {
        deletePublicKey(environment, environment.getAuthentication().getPublicKeyId());
    }

    public void deletePublicKey(Environment environment, String publicKeyId) {
        try {
            LOGGER.info("Try to delete the ssh key ({}) for {}", publicKeyId, environment.getName());
            PublicKeyConnector publicKeyConnector = getPublicKeyConnector(environment.getCloudPlatform())
                    .orElseThrow(() -> new BadRequestException("No public key connector for cloud platform: " + environment.getCloudPlatform()));
            PublicKeyUnregisterRequest request = createPublicKeyUnregisterRequest(environment, publicKeyId);
            publicKeyConnector.unregister(request);
            LOGGER.info("the ssh key ({}) deleted successfully for {}", publicKeyId, environment.getName());
        } catch (UnsupportedOperationException e) {
            LOGGER.info("Cloud platform {} does not support public key services", environment.getCloudPlatform());
        }
    }

    public boolean isPublicKeyIdExists(Environment environment, String publicKeyId) {
        PublicKeyConnector publicKeyConnector = getPublicKeyConnector(environment.getCloudPlatform())
                .orElseThrow(() -> new BadRequestException("No network connector for cloud platform: " + environment.getCloudPlatform()));
        return publicKeyConnector.exists(createPublicKeyDescribeRequest(environment, publicKeyId));
    }

    private PublicKeyUnregisterRequest createPublicKeyUnregisterRequest(Environment environment, String publicKeyId) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        return PublicKeyUnregisterRequest.builder()
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(cloudCredential)
                .withPublicKeyId(publicKeyId)
                .withRegion(environment.getLocation())
                .build();
    }

    private PublicKeyDescribeRequest createPublicKeyDescribeRequest(Environment environment, String publicKeyId) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        return PublicKeyDescribeRequest.builder()
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(cloudCredential)
                .withPublicKeyId(publicKeyId)
                .withRegion(environment.getLocation())
                .build();
    }

    public boolean isExistingSshKeyUpdateSupported(Environment environment) {
        return supportedExistingSshKeyUpdateProviders.stream().anyMatch(s -> s.equalsIgnoreCase(environment.getCloudPlatform()));
    }

    public boolean isRawSshKeyUpdateSupported(Environment environment) {
        return supportedRawSshKeyUpdateProviders.stream().anyMatch(s -> s.equalsIgnoreCase(environment.getCloudPlatform()));
    }

    public Optional<EncryptionProfile> getEncryptionProfile(String encryptionProfileCrn) {
        EncryptionProfile encryptionProfile = null;

        if (StringUtils.isNotEmpty(encryptionProfileCrn)) {
            try {
                encryptionProfile = encryptionProfileService.getByCrnOrDefault(encryptionProfileCrn);
            } catch (NotFoundException e) {
                throw new BadRequestException(String.format("No Encryption Profile found with CRN [%s].", encryptionProfileCrn), e);
            }
        }

        return Optional.ofNullable(encryptionProfile);
    }
}
