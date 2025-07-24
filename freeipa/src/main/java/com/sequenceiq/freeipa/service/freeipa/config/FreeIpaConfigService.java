package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.dns.HybridReverseDnsZoneCalculator;
import com.sequenceiq.freeipa.service.freeipa.dns.ReverseDnsZoneCalculator;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.NetworkService;

@Service
public class FreeIpaConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaConfigService.class);

    private static final String DEFAULT_DNSSEC_VALIDATION_PREFIX = "freeipa.platform.dnssec.validation.";

    @Value("${freeipa.kerberos.secret.location}")
    private String kerberosSecretLocation;

    @Inject
    private NetworkService networkService;

    @Inject
    private ReverseDnsZoneCalculator reverseDnsZoneCalculator;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private Environment environment;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private FreeIpaLoadBalancerService loadBalancerService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Inject
    private HybridReverseDnsZoneCalculator hybridReverseDnsZoneCalculator;

    public FreeIpaConfigView createFreeIpaConfigs(Stack stack, Set<Node> hosts) {
        final FreeIpaConfigView.Builder builder = new FreeIpaConfigView.Builder();

        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        Multimap<String, String> subnetWithCidr = networkService.getFilteredSubnetWithCidr(stack);
        LOGGER.debug("Subnets for reverse zone calculation : {}", subnetWithCidr);
        DetailedEnvironmentResponse environmentResponse = cachedEnvironmentClientService.getByCrn(stack.getEnvironmentCrn());
        String reverseZones = EnvironmentType.isHybridFromEnvironmentTypeString(environmentResponse.getEnvironmentType()) ?
                hybridReverseDnsZoneCalculator.reverseDnsZoneForCidrs(subnetWithCidr.values()) :
                reverseDnsZoneCalculator.reverseDnsZoneForCidrs(subnetWithCidr.values());
        LOGGER.debug("Reverse zones : {}", reverseZones);
        String seLinux = null != stack.getSecurityConfig() && null != stack.getSecurityConfig().getSeLinux() ?
                stack.getSecurityConfig().getSeLinux().toString().toLowerCase(Locale.ROOT) : SeLinux.PERMISSIVE.toString().toLowerCase(Locale.ROOT);
        return builder
                .withRealm(freeIpa.getDomain().toUpperCase(Locale.ROOT))
                .withDomain(freeIpa.getDomain())
                .withPassword(freeIpa.getAdminPassword())
                .withDnssecValidationEnabled(isDnsSecValidationEnabled(stack.getCloudPlatform()))
                .withReverseZones(reverseZones)
                .withAdminUser(freeIpaClientFactory.getAdminUser())
                .withFreeIpaToReplicate(gatewayConfigService.getPrimaryGatewayConfig(stack))
                .withHosts(hosts)
                .withBackupConfig(determineAndSetBackup(stack))
                .withCcmv2Enabled(stack.getTunnel().useCcmV2OrJumpgate())
                .withCidrBlocks(stack.getNetwork().getNetworkCidrs())
                .withCcmv2JumpgateEnabled(stack.getTunnel().useCcmV2Jumpgate())
                .withSecretEncryptionEnabled(environmentService.isSecretEncryptionEnabled(stack.getEnvironmentCrn()))
                .withKerberosSecretLocation(kerberosSecretLocation)
                .withSeLinux(seLinux)
                .withEncryptionConfig(new FreeIpaEncryptionConfigView(environmentResponse.getEncryptionProfile()))
                .withLbConfig(loadBalancerService.findByStackId(stack.getId())
                        .map(lb -> new FreeIpaLbConfigView(lb.getEndpoint(), lb.getFqdn(), lb.getIp()))
                        .orElse(new FreeIpaLbConfigView()))
                .build();
    }

    private boolean isDnsSecValidationEnabled(String cloudPlatform) {
        String dnssecValidationPropertyKey =
                DEFAULT_DNSSEC_VALIDATION_PREFIX + cloudPlatform;
        if (!environment.containsProperty(dnssecValidationPropertyKey)) {
            LOGGER.debug("{} property is not set. Defaulting to true",
                    dnssecValidationPropertyKey);
        }
        boolean dnsSecValidationEnabled = Boolean.parseBoolean(environment.getProperty(dnssecValidationPropertyKey, "true"));
        LOGGER.info("DNSSEC validation is {}", dnsSecValidationEnabled ? "enabled" : "disabled");
        return dnsSecValidationEnabled;
    }

    private FreeIpaBackupConfigView determineAndSetBackup(Stack stack) {
        Backup backup = stack.getBackup();
        final FreeIpaBackupConfigView.Builder builder = new FreeIpaBackupConfigView.Builder();
        if (backup != null) {
            builder.withEnabled(true)
                    .withMonthlyFullEnabled(backup.isMonthlyFullEnabled())
                    .withHourlyEnabled(backup.isHourlyEnabled())
                    .withInitialFullEnabled(backup.isInitialFullEnabled())
                    .withLocation(backup.getStorageLocation());
            if (backup.getS3() != null) {
                builder.withPlatform(CloudPlatform.AWS.name());
                builder.withAwsRegion(stack.getRegion());
                LOGGER.debug("Backups will be configured to use S3 storage in {} region.", stack.getRegion());
            } else if (backup.getAdlsGen2() != null) {
                builder.withPlatform(CloudPlatform.AZURE.name())
                        .withAzureInstanceMsi(backup.getAdlsGen2().getManagedIdentity());
                LOGGER.debug("Backups will be configured to use Azure Blob storage");
            } else if (backup.getGcs() != null) {
                builder.withPlatform(CloudPlatform.GCP.name())
                        .withGcpServiceAccount(backup.getGcs().getServiceAccountEmail());
                LOGGER.debug("Backups will be configured to use GCP storage");
            }
            Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByEnvironmentCrn(stack.getEnvironmentCrn());
            proxyConfig.ifPresent(config -> {
                LOGGER.debug("Proxy will be configured for backup: {}", config.getName());
                builder.withProxyUrl(config.getFullProxyUrl());
            });
        } else {
            builder.withEnabled(false);
            LOGGER.debug("Backups will not be configured.");
        }
        return builder.build();
    }
}
