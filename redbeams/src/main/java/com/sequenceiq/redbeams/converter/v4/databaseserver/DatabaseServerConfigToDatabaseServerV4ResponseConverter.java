package com.sequenceiq.redbeams.converter.v4.databaseserver;

import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode.DISABLED;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode.ENABLED;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType.NONE;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@Component
public class DatabaseServerConfigToDatabaseServerV4ResponseConverter {

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    public DatabaseServerV4Response convert(DatabaseServerConfig source) {
        DatabaseServerV4Response response = new DatabaseServerV4Response();
        response.setId(source.getId());
        response.setCrn(source.getResourceCrn().toString());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setHost(source.getHost());
        response.setPort(source.getPort());

        response.setDatabaseVendor(source.getDatabaseVendor().databaseType());
        response.setDatabaseVendorDisplayName(source.getDatabaseVendor().displayName());
        response.setConnectionDriver(source.getConnectionDriver());
        response.setConnectionUserName(stringToSecretResponseConverter.convert(source.getConnectionUserNameSecret()));
        response.setConnectionPassword(stringToSecretResponseConverter.convert(source.getConnectionPasswordSecret()));
        response.setCreationDate(source.getCreationDate());
        response.setEnvironmentCrn(source.getEnvironmentId());
        response.setClusterCrn(source.getClusterCrn());

        response.setResourceStatus(source.getResourceStatus());
        if (source.getDbStack().isPresent()) {
            DBStack dbStack = source.getDbStack().get();
            response.setStatus(dbStack.getStatus());
            response.setStatusReason(dbStack.getStatusReason());
            if (dbStack.getSslConfig() != null) {
                SslConfig sslConfig = dbStack.getSslConfig();
                SslConfigV4Response sslConfigV4Response = new SslConfigV4Response();
                sslConfigV4Response.setSslCertificates(sslConfig.getSslCertificates());
                sslConfigV4Response.setSslCertificateType(sslConfig.getSslCertificateType());
                sslConfigV4Response.setSslMode(NONE.equals(sslConfig.getSslCertificateType()) ? DISABLED : ENABLED);
                String cloudPlatform = dbStack.getCloudPlatform();
                // TODO Add SslConfig.sslCertificateMaxVersion that is kept up-to-date (mostly for GCP), use getMaxVersionByPlatform() as fallback
                sslConfigV4Response.setSslCertificateHighestAvailableVersion(databaseServerSslCertificateConfig.getMaxVersionByPlatform(cloudPlatform));
                sslConfigV4Response.setSslCertificateActiveVersion(Optional.ofNullable(sslConfig.getSslCertificateActiveVersion())
                        .orElse(databaseServerSslCertificateConfig.getLegacyMaxVersionByPlatform(cloudPlatform)));
                sslConfigV4Response.setSslCertificateActiveCloudProviderIdentifier(
                        Optional.ofNullable(sslConfig.getSslCertificateActiveCloudProviderIdentifier())
                                .orElse(databaseServerSslCertificateConfig.getLegacyCloudProviderIdentifierByPlatform(cloudPlatform)));
                response.setSslConfig(sslConfigV4Response);
            }
        } else if (source.getHost() != null && source.getPort() != null) {
            response.setStatus(Status.AVAILABLE);
        } else {
            response.setStatus(Status.UNKNOWN);
        }
        if (response.getSslConfig() == null) {
            response.setSslConfig(new SslConfigV4Response());
        }

        return response;
    }

}
