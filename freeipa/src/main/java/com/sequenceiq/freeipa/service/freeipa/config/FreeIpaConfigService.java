package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.dns.ReverseDnsZoneCalculator;
import com.sequenceiq.freeipa.service.stack.NetworkService;

@Service
public class FreeIpaConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaConfigService.class);

    private static final String DEFAULT_DNSSEC_VALIDATION_PREFIX = "freeipa.platform.dnssec.validation.";

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

    public FreeIpaConfigView createFreeIpaConfigs(Stack stack, Set<Node> hosts, ProxyConfig proxyConfig) {
        final FreeIpaConfigView.Builder builder = new FreeIpaConfigView.Builder();

        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        Map<String, String> subnetWithCidr = networkService.getFilteredSubnetWithCidr(stack);
        String reverseZones = reverseDnsZoneCalculator.reverseDnsZoneForCidrs(subnetWithCidr.values());

        return builder
                .withRealm(freeIpa.getDomain().toUpperCase())
                .withDomain(freeIpa.getDomain())
                .withPassword(freeIpa.getAdminPassword())
                .withDnssecValidationEnabled(isDnsSecValidationEnabled(stack.getCloudPlatform()))
                .withReverseZones(reverseZones)
                .withAdminUser(freeIpaClientFactory.getAdminUser())
                .withFreeIpaToReplicate(gatewayConfigService.getPrimaryGatewayConfig(stack).getHostname())
                .withHosts(hosts)
                .withBackupConfig(determineAndSetBackup(stack, proxyConfig))
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

    private FreeIpaBackupConfigView determineAndSetBackup(Stack stack, ProxyConfig proxyConfig) {
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
            }
            if (proxyConfig != null) {
                LOGGER.debug("Proxy will be configured for backup: {}", proxyConfig.getName());
                builder.withProxyUrl(proxyConfig.getFullProxyUrl());
            }
        } else {
            builder.withEnabled(false);
            LOGGER.debug("Backups will not be configured.");
        }
        return builder.build();
    }
}
