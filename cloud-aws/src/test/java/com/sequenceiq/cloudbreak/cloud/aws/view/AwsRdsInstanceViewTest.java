package com.sequenceiq.cloudbreak.cloud.aws.view;

import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsInstanceView.BACKUP_RETENTION_PERIOD;
import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsInstanceView.ENGINE_VERSION;
import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsInstanceView.MULTI_AZ;
import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsInstanceView.STORAGE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

@ExtendWith(MockitoExtension.class)
public class AwsRdsInstanceViewTest {

    private static final String SSL_CERTIFICATE_IDENTIFIER = "mycert";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DatabaseServer server;

    @InjectMocks
    private AwsRdsInstanceView underTest;

    @Test
    public void testAllocatedStorage() {
        when(server.getStorageSize()).thenReturn(50L);
        assertEquals(50L, underTest.getAllocatedStorage());
    }

    @Test
    public void testBackupRetentionPeriod() {
        when(server.getParameter(BACKUP_RETENTION_PERIOD, Integer.class)).thenReturn(Integer.valueOf("3"));
        assertEquals(3, underTest.getBackupRetentionPeriod());
    }

    @Test
    public void testNoBackupRetentionPeriod() {
        when(server.getParameter(BACKUP_RETENTION_PERIOD, Integer.class)).thenReturn(null);
        assertNull(underTest.getBackupRetentionPeriod());
    }

    @Test
    public void testDBInstanceClass() {
        when(server.getFlavor()).thenReturn("db.m3.medium");
        assertEquals("db.m3.medium", underTest.getDBInstanceClass());
    }

    @Test
    public void testNoDBInstanceClass() {
        when(server.getFlavor()).thenReturn(null);
        assertNull(underTest.getDBInstanceClass());
    }

    @Test
    public void testDBInstanceIdentifier() {
        when(server.getServerId()).thenReturn("myserver");
        assertEquals("myserver", underTest.getDBInstanceIdentifier());
    }

    @Test
    public void testNoDBInstanceIdentifier() {
        when(server.getServerId()).thenReturn(null);
        assertNull(underTest.getDBInstanceIdentifier());
    }

    @Test
    public void testMasterUsername() {
        when(server.getRootUserName()).thenReturn("root");
        assertEquals("root", underTest.getMasterUsername());
    }

    @Test
    public void testEnginePostgres() {
        when(server.getEngine()).thenReturn(DatabaseEngine.POSTGRESQL);
        assertEquals("postgres", underTest.getEngine());
    }

    @Test
    public void testNoEngine() {
        when(server.getEngine()).thenReturn(null);
        assertNull(underTest.getEngine());
    }

    @Test
    public void testEngineVersion() {
        when(server.getStringParameter(ENGINE_VERSION)).thenReturn("1.2.3");
        assertEquals("1.2.3", underTest.getEngineVersion());
    }

    @Test
    public void testNoEngineVersion() {
        when(server.getStringParameter(ENGINE_VERSION)).thenReturn(null);
        assertNull(underTest.getEngineVersion());
    }

    @Test
    public void testNoMasterUsername() {
        when(server.getRootUserName()).thenReturn(null);
        assertNull(underTest.getMasterUsername());
    }

    @Test
    public void testMasterUserPassword() {
        when(server.getRootPassword()).thenReturn("cloudera");
        assertEquals("cloudera", underTest.getMasterUserPassword());
    }

    @Test
    public void testNoMasterUserPassword() {
        when(server.getRootPassword()).thenReturn(null);
        assertNull(underTest.getMasterUserPassword());
    }

    @Test
    public void testMultiAZ() {
        when(server.getStringParameter(MULTI_AZ)).thenReturn("true");
        assertEquals("true", underTest.getMultiAZ());
    }

    @Test
    public void testNoMultiAZ() {
        when(server.getStringParameter(MULTI_AZ)).thenReturn(null);
        assertNull(underTest.getMultiAZ());
    }

    @Test
    public void testStorageType() {
        when(server.getStringParameter(STORAGE_TYPE)).thenReturn("gp2");
        assertEquals("gp2", underTest.getStorageType());
    }

    @Test
    public void testNoStorageType() {
        when(server.getStringParameter(STORAGE_TYPE)).thenReturn(null);
        assertNull(underTest.getStorageType());
    }

    @Test
    public void testVPCSecurityGroups() {
        when(server.getSecurity().getCloudSecurityIds()).thenReturn(List.of("sg-123"));
        assertEquals(List.of("sg-123"), underTest.getVPCSecurityGroups());
    }

    @Test
    public void testNoVPCSecurityGroups() {
        when(server.getSecurity().getCloudSecurityIds()).thenReturn(null);
        assertNull(underTest.getVPCSecurityGroups());
    }

    @Test
    void isSslCertificateIdentifierDefinedTestWhenAbsent() {
        when(server.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).thenReturn(null);

        assertThat(underTest.isSslCertificateIdentifierDefined()).isFalse();
    }

    @Test
    void isSslCertificateIdentifierDefinedTestWhenEmpty() {
        when(server.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).thenReturn("");

        assertThat(underTest.isSslCertificateIdentifierDefined()).isFalse();
    }

    @Test
    void isSslCertificateIdentifierDefinedTestWhenPresent() {
        when(server.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).thenReturn(SSL_CERTIFICATE_IDENTIFIER);

        assertThat(underTest.isSslCertificateIdentifierDefined()).isTrue();
    }

    @Test
    void getSslCertificateIdentifierTestWhenAbsent() {
        when(server.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).thenReturn(null);

        assertThat(underTest.getSslCertificateIdentifier()).isNull();
    }

    @Test
    void getSslCertificateIdentifierTestWhenEmpty() {
        when(server.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).thenReturn("");

        assertThat(underTest.getSslCertificateIdentifier()).isEqualTo("");
    }

    @Test
    void getSslCertificateIdentifierTestWhenPresent() {
        when(server.getStringParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER)).thenReturn(SSL_CERTIFICATE_IDENTIFIER);

        assertThat(underTest.getSslCertificateIdentifier()).isEqualTo(SSL_CERTIFICATE_IDENTIFIER);
    }

}
