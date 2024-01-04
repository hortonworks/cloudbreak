package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.io.IOException;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.SslCert;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType;

@Service
public class GcpDatabaseServerCertificateService extends GcpDatabaseServerBaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabaseServerCertificateService.class);

    @Inject
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    public CloudDatabaseServerSslCertificate getActiveSslRootCertificate(AuthenticatedContext ac, DatabaseStack databaseStack) throws IOException {
        SQLAdmin sqlAdmin = gcpSQLAdminFactory.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());
        String projectId = gcpStackUtil.getProjectId(ac.getCloudCredential());
        DatabaseServer databaseServer = databaseStack.getDatabaseServer();
        String serverId = databaseServer.getServerId();
        LOGGER.info("Fetch GCP Database instance {}", serverId);
        DatabaseInstance instance = sqlAdmin.instances().get(projectId, serverId).execute();
        SslCert serverCaCert = instance.getServerCaCert();
        CloudDatabaseServerSslCertificate result = null;
        if (serverCaCert == null) {
            LOGGER.info("No GCP Database cert found for {}", serverId);
        } else {
            LOGGER.info("Returned GCP Database cert {}", serverCaCert.getCommonName());
            String certId = String.format("%s_%s", serverCaCert.getCreateTime(), serverCaCert.getExpirationTime());
            result = new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, certId, serverCaCert.getCert());
        }
        return result;
    }

}
