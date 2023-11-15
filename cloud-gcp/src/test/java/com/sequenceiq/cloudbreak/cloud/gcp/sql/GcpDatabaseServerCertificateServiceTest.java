package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.SQLAdmin.Instances;
import com.google.api.services.sqladmin.SQLAdmin.Instances.Get;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.SslCert;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType;

@ExtendWith(MockitoExtension.class)
class GcpDatabaseServerCertificateServiceTest {

    private static final String PROJECT_ID = "cloudbreak";

    private static final String DB_SERVER_ID = "db";

    private static final String CERT = "-----BEGIN CERTIFICATE-----";

    @Mock
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private SQLAdmin sqlAdmin;

    @InjectMocks
    private GcpDatabaseServerCertificateService underTest;

    @BeforeEach
    public void initTests() {
        when(cloudCredential.getName()).thenReturn(PROJECT_ID);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn(PROJECT_ID);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpSQLAdminFactory.buildSQLAdmin(cloudCredential, PROJECT_ID)).thenReturn(sqlAdmin);
    }

    @Test
    public void testGetActiveSslRootCertificateAndReturnValidCert() throws IOException {
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        Instances instances = mock(Instances.class);
        Get get = mock(Get.class);
        DatabaseInstance instance = mock(DatabaseInstance.class);
        SslCert sslCert = mock(SslCert.class);
        when(databaseServer.getServerId()).thenReturn(DB_SERVER_ID);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(get.execute()).thenReturn(instance);
        when(instances.get(PROJECT_ID, DB_SERVER_ID)).thenReturn(get);
        when(sqlAdmin.instances()).thenReturn(instances);
        when(instance.getServerCaCert()).thenReturn(sslCert);
        when(sslCert.getCert()).thenReturn(CERT);
        when(sslCert.getCommonName()).thenReturn("Google_CA");
        when(sslCert.getCreateTime()).thenReturn("2023");
        when(sslCert.getExpirationTime()).thenReturn("2033");

        CloudDatabaseServerSslCertificate result = underTest.getActiveSslRootCertificate(authenticatedContext, databaseStack);
        assertEquals(CERT, result.certificate());
        assertEquals("2023_2033", result.certificateIdentifier());
        assertEquals(CloudDatabaseServerSslCertificateType.ROOT, result.certificateType());
    }

    @Test
    public void testGetActiveSslRootCertificateAndNoCertIsConfigured() throws IOException {
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        Instances instances = mock(Instances.class);
        Get get = mock(Get.class);
        DatabaseInstance instance = mock(DatabaseInstance.class);
        when(databaseServer.getServerId()).thenReturn(DB_SERVER_ID);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(get.execute()).thenReturn(instance);
        when(instances.get(PROJECT_ID, DB_SERVER_ID)).thenReturn(get);
        when(sqlAdmin.instances()).thenReturn(instances);
        when(instance.getServerCaCert()).thenReturn(null);

        CloudDatabaseServerSslCertificate result = underTest.getActiveSslRootCertificate(authenticatedContext, databaseStack);
        assertNull(result);
    }

}