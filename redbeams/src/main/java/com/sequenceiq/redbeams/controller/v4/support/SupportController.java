package com.sequenceiq.redbeams.controller.v4.support;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.cloud.model.DefaultPlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.support.CertificateSwapV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.support.CertificateSwapV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.support.RedBeamsPlatformSupportRequirements;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.converter.v4.SslCertificateEntryToSslCertificateEntryResponseConverter;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class SupportController implements SupportV4Endpoint {

    private static final String PROVIDER = "mock";

    private static final String REGION = "london";

    private static final String BACKUP = "nodnol";

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Inject
    private SslCertificateEntryToSslCertificateEntryResponseConverter sslCertificateEntryToSslCertificateEntryResponseConverter;

    @Inject
    private CloudParameterService cloudParameterService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATABASE_SERVER)
    public CertificateSwapV4Response swapCertificate(@RequestObject CertificateSwapV4Request request) {
        Set<SslCertificateEntry> backup = databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(PROVIDER, BACKUP);
        if (backup == null || backup.isEmpty()) {
            backup = databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(PROVIDER, REGION);
            databaseServerSslCertificateConfig.modifyMockProviderCertCache(BACKUP, backup);
        }
        Set<SslCertificateEntry> chosenCerts = new HashSet<>();
        if (request.getFirstCert()) {
            chosenCerts.add(backup.stream().filter(c -> c.version() == 0).findAny().get());
        }
        if (request.getSecondCert()) {
            chosenCerts.add(backup.stream().filter(c -> c.version() == 1).findAny().get());
        }
        databaseServerSslCertificateConfig.modifyMockProviderCertCache(REGION, chosenCerts);

        return null;
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public SslCertificateEntryResponse getLatestCertificate(String cloudPlatform, String region) {
        int maxVersionByCloudPlatformAndRegion = databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(cloudPlatform, region);
        SslCertificateEntry entry = databaseServerSslCertificateConfig
                .getCertByCloudPlatformAndRegionAndVersion(cloudPlatform, region, maxVersionByCloudPlatformAndRegion);
        if (entry != null) {
            return sslCertificateEntryToSslCertificateEntryResponseConverter.convert(entry);
        }
        throw new BadRequestException(String.format("Could not found latest certificate for %s cloud and %s region.",
                cloudPlatform,
                region));
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public RedBeamsPlatformSupportRequirements getInstanceTypesByPlatform(String cloudPlatform) {
        RedBeamsPlatformSupportRequirements requirements = new RedBeamsPlatformSupportRequirements();
        if (StringUtils.isNotBlank(cloudPlatform)) {
            DefaultPlatformDatabaseCapabilities defaultDatabaseCapabilities =
                    cloudParameterService.getDefaultDatabaseCapabilities(cloudPlatform.toLowerCase(Locale.ROOT));
            requirements.setDefaultX86InstanceTypeRequirements(
                    defaultDatabaseCapabilities.getDefaultX86InstanceTypeRequirements());
            requirements.setDefaultArmInstanceTypeRequirements(
                    defaultDatabaseCapabilities.getDefaultArmInstanceTypeRequirements());
        }
        return requirements;
    }
}
