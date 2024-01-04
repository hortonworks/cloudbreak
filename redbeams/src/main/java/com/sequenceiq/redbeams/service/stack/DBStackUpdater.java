package com.sequenceiq.redbeams.service.stack;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.service.sslcertificate.SslConfigService;

@Service
public class DBStackUpdater {

    private static final Logger LOGGER = getLogger(DBStackUpdater.class);

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private SslConfigService sslConfigService;

    public void updateSslConfig(long id) {
        Optional<DBStack> dbStackOpt = dbStackService.findById(id);
        dbStackOpt.ifPresent(dbStack -> {
            Optional<SslConfig> sslConfig = sslConfigService.fetchById(dbStack.getSslConfig());
            if (sslConfig.isPresent() && sslConfig.get().getSslCertificateType() == SslCertificateType.CLOUD_PROVIDER_OWNED
                    && databaseServerSslCertificateConfig.isCloudPlatformAndRegionSupportedForCerts(dbStack.getCloudPlatform(), dbStack.getRegion())) {
                String cloudPlatform = dbStack.getCloudPlatform();
                String region = dbStack.getRegion();
                SslConfig sslConf = sslConfig.get();
                Set<SslCertificateEntry> allCertificates = databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(cloudPlatform, region);
                sslConf.setSslCertificates(allCertificates.stream().map(SslCertificateEntry::certPem).collect(Collectors.toSet()));
                int activeVersion = databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(cloudPlatform, region);
                SslCertificateEntry activeSslCert = allCertificates.stream()
                        .filter(sslCert -> sslCert.version() == activeVersion)
                        .findFirst()
                        .orElseThrow(NotFoundException.notFound(String.format("Active SSL cert cannot be found for %s", dbStack.getName())));
                sslConf.setSslCertificateActiveVersion(activeVersion);
                sslConf.setSslCertificateActiveCloudProviderIdentifier(activeSslCert.cloudProviderIdentifier());
                sslConfigService.save(sslConf);
            } else {
                String sslNullPrefix = "";
                String sslTypeString = "null";
                if (sslConfig.isPresent()) {
                    sslNullPrefix = "not ";
                    sslTypeString = sslConfig.get().getSslCertificateType().name();
                }
                LOGGER.debug("SSL config will be untouched, SSL is {}null and cert type is {}", sslNullPrefix, sslTypeString);
            }
        });
    }
}
