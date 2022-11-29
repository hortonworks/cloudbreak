package com.sequenceiq.redbeams.service.stack;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@Service
public class DBStackUpdater {

    private static final Logger LOGGER = getLogger(DBStackUpdater.class);

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Inject
    private DBStackService dbStackService;

    public void updateSslConfig(long id) {
        Optional<DBStack> dbStackOpt = dbStackService.findById(id);
        dbStackOpt.ifPresent(dbStack -> {
            SslConfig sslConfig = dbStack.getSslConfig();
            if (sslConfig != null && sslConfig.getSslCertificateType() == SslCertificateType.CLOUD_PROVIDER_OWNED) {
                String cloudPlatform = dbStack.getCloudPlatform();
                String region = dbStack.getRegion();
                Set<SslCertificateEntry> allCertificates = databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(cloudPlatform, region);
                sslConfig.setSslCertificates(allCertificates.stream().map(SslCertificateEntry::getCertPem).collect(Collectors.toSet()));
                int activeVersion = databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(cloudPlatform, region);
                SslCertificateEntry activeSslCert = allCertificates.stream()
                        .filter(sslCert -> sslCert.getVersion() == activeVersion)
                        .findFirst()
                        .orElseThrow(NotFoundException.notFound(String.format("Active SSL cert cannot be found for %s", dbStack.getName())));
                sslConfig.setSslCertificateActiveVersion(activeVersion);
                sslConfig.setSslCertificateActiveCloudProviderIdentifier(activeSslCert.getCloudProviderIdentifier());
                dbStackService.save(dbStack);
            } else {
                String sslNullPrefix = "";
                String sslTypeString = "null";
                if (sslConfig != null) {
                    sslNullPrefix = "not ";
                    sslTypeString = sslConfig.getSslCertificateType().name();
                }
                LOGGER.debug("SSL config will be untouched, SSL is {}null and cert type is {}", sslNullPrefix, sslTypeString);
            }
        });
    }
}
