package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerSslCertificateType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerSslConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerSslMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

@Component
public class DatabaseServerConverter {

    public StackDatabaseServerResponse convert(DatabaseServerV4Response databaseServerV4Response) {
        StackDatabaseServerResponse stackDatabaseServerResponse = new StackDatabaseServerResponse();
        stackDatabaseServerResponse.setCrn(databaseServerV4Response.getCrn());
        stackDatabaseServerResponse.setName(databaseServerV4Response.getName());
        stackDatabaseServerResponse.setDescription(databaseServerV4Response.getDescription());
        stackDatabaseServerResponse.setEnvironmentCrn(databaseServerV4Response.getEnvironmentCrn());
        stackDatabaseServerResponse.setHost(databaseServerV4Response.getHost());
        stackDatabaseServerResponse.setPort(databaseServerV4Response.getPort());
        stackDatabaseServerResponse.setDatabaseVendor(databaseServerV4Response.getDatabaseVendor());
        stackDatabaseServerResponse.setDatabaseVendorDisplayName(databaseServerV4Response.getDatabaseVendorDisplayName());
        stackDatabaseServerResponse.setCreationDate(databaseServerV4Response.getCreationDate());
        if (databaseServerV4Response.getResourceStatus() != null) {
            stackDatabaseServerResponse.setResourceStatus(resourceStatusToDatabaseServerResourceStatus(databaseServerV4Response.getResourceStatus()));
        }
        if (databaseServerV4Response.getStatus() != null) {
            stackDatabaseServerResponse.setStatus(statusToDatabaseServerStatus(databaseServerV4Response.getStatus()));
        }
        stackDatabaseServerResponse.setStatusReason(databaseServerV4Response.getStatusReason());
        stackDatabaseServerResponse.setClusterCrn(databaseServerV4Response.getClusterCrn());
        if (databaseServerV4Response.getSslConfig() != null) {
            SslConfigV4Response sslConfig = databaseServerV4Response.getSslConfig();
            DatabaseServerSslConfig databaseServerSslConfig = new DatabaseServerSslConfig();
            databaseServerSslConfig.setSslCertificates(sslConfig.getSslCertificates());
            if (sslConfig.getSslMode() != null) {
                databaseServerSslConfig.setSslMode(sslModeToDatabaseServerSslMode(sslConfig.getSslMode()));
            }
            if (sslConfig.getSslCertificateType() != null) {
                databaseServerSslConfig.setSslCertificateType(sslCertificateTypeToDatabaseServerSslCertificateType(sslConfig.getSslCertificateType()));
            }
            stackDatabaseServerResponse.setSslConfig(databaseServerSslConfig);
        }
        return stackDatabaseServerResponse;
    }

    private DatabaseServerResourceStatus resourceStatusToDatabaseServerResourceStatus(ResourceStatus resourceStatus) {
        return DatabaseServerResourceStatus.valueOf(resourceStatus.name());
    }

    private DatabaseServerStatus statusToDatabaseServerStatus(Status status) {
        return DatabaseServerStatus.valueOf(status.name());
    }

    private DatabaseServerSslMode sslModeToDatabaseServerSslMode(SslMode sslMode) {
        return DatabaseServerSslMode.valueOf(sslMode.name());
    }

    private DatabaseServerSslCertificateType sslCertificateTypeToDatabaseServerSslCertificateType(SslCertificateType sslCertificateType) {
        return DatabaseServerSslCertificateType.valueOf(sslCertificateType.name());
    }

}
