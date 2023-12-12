package com.sequenceiq.redbeams.controller.v4.support;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.redbeams.api.endpoint.v4.support.CertificateSwapV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.support.CertificateSwapV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class SupportController implements SupportV4Endpoint {

    private static final String PROVIDER = "mock";

    private static final String REGION = "london";

    private static final String BACKUP = "nodnol";

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

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
}
