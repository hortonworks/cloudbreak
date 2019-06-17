package com.sequenceiq.cloudbreak.cloud.aws.view;

import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsInstanceView.BACKUP_RETENTION_PERIOD;
import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsRdsInstanceView.ENGINE_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

public class AwsRdsInstanceViewTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DatabaseServer server;

    private AwsRdsInstanceView underTest;

    @Before
    public void setUp() {
        initMocks(this);

        underTest = new AwsRdsInstanceView(server);
    }

    @Test
    public void testAllocatedStorage() {
        when(server.getStorageSize()).thenReturn(50L);
        assertEquals(50L, underTest.getAllocatedStorage().longValue());
    }

    @Test
    public void testBackupRetentionPeriod() {
        when(server.getParameter(BACKUP_RETENTION_PERIOD, Integer.class)).thenReturn(Integer.valueOf("3"));
        assertEquals(3, underTest.getBackupRetentionPeriod().intValue());
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
    public void testVPCSecurityGroups() {
        when(server.getSecurity().getCloudSecurityIds()).thenReturn(List.of("sg-123"));
        assertEquals(List.of("sg-123"), underTest.getVPCSecurityGroups());
    }

    @Test
    public void testNoVPCSecurityGroups() {
        when(server.getSecurity().getCloudSecurityIds()).thenReturn(null);
        assertNull(underTest.getVPCSecurityGroups());
    }
}
