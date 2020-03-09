package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.backup.cloud.AdlsGen2BackupConfigGenerator;
import com.sequenceiq.freeipa.service.freeipa.backup.cloud.S3BackupConfigGenerator;
import com.sequenceiq.freeipa.service.freeipa.dns.ReverseDnsZoneCalculator;
import com.sequenceiq.freeipa.service.stack.NetworkService;

@Service
public class FreeIpaConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaConfigService.class);

    @Inject
    private S3BackupConfigGenerator s3BackupConfigGenerator;

    @Inject
    private AdlsGen2BackupConfigGenerator adlsGen2BackupConfigGenerator;

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

    public FreeIpaConfigView createFreeIpaConfigs(Stack stack, Set<Node> hosts) {
        final FreeIpaConfigView.Builder builder = new FreeIpaConfigView.Builder();

        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        Map<String, String> subnetWithCidr = networkService.getFilteredSubnetWithCidr(stack);
        String reverseZones = reverseDnsZoneCalculator.reverseDnsZoneForCidrs(subnetWithCidr.values());

        return builder
                .withRealm(freeIpa.getDomain().toUpperCase())
                .withDomain(freeIpa.getDomain())
                .withPassword(freeIpa.getAdminPassword())
                .withReverseZones(reverseZones)
                .withAdminUser(freeIpaClientFactory.getAdminUser())
                .withFreeIpaToReplicate(gatewayConfigService.getPrimaryGatewayConfig(stack).getHostname())
                .withHosts(hosts)
                .withBackupConfig(determineAndSetBackup(stack))
                .build();
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
                LOGGER.debug("Backups will be configured to use S3 output.");
            } else if (backup.getAdlsGen2() != null) {
                builder.withPlatform(CloudPlatform.AZURE.name())
                        .withAzureInstanceMsi(backup.getAdlsGen2().getManagedIdentity());
                LOGGER.debug("Backups will be configured to use Azure Blob storage");
            }
        } else {
            builder.withEnabled(false);
            LOGGER.debug("Backups will not be configured.");
        }
        return builder.build();
    }
}
