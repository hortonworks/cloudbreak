package com.sequenceiq.cloudbreak.cloud.azure.view;

import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.DB_VERSION;
import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.BACKUP_RETENTION_DAYS;
import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.GEO_REDUNDANT_BACKUP;
import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.SKU_CAPACITY;
import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.SKU_FAMILY;
import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.SKU_TIER;
import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.STORAGE_AUTO_GROW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

public class AzureDatabaseServerViewTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DatabaseServer server;

    private AzureDatabaseServerView underTest;

    @Before
    public void setUp() {
        initMocks(this);

        underTest = new AzureDatabaseServerView(server);
    }

    @Test
    public void testDbVersion() {
        when(server.getStringParameter(DB_VERSION)).thenReturn("1234");
        assertThat(underTest.getDbVersion()).isEqualTo("1234");
    }

    @Test
    public void testBackupRetentionDays() {
        when(server.getParameter(BACKUP_RETENTION_DAYS, Integer.class)).thenReturn(5);
        assertThat(underTest.getBackupRetentionDays()).isEqualTo(5);
    }

    @Test
    public void testGeoRedundantBackup() {
        when(server.getParameter(GEO_REDUNDANT_BACKUP, Boolean.class)).thenReturn(true);
        assertThat(underTest.getGeoRedundantBackup()).isEqualTo(true);
    }

    @Test
    public void testSkuCapacity() {
        when(server.getParameter(SKU_CAPACITY, Integer.class)).thenReturn(4);
        assertThat(underTest.getSkuCapacity()).isEqualTo(4);
    }

    @Test
    public void testSkuFamily() {
        when(server.getStringParameter(SKU_FAMILY)).thenReturn("some-family");
        assertThat(underTest.getSkuFamily()).isEqualTo("some-family");
    }

    @Test
    public void testSkuTier() {
        when(server.getStringParameter(SKU_TIER)).thenReturn("some-tier");
        assertThat(underTest.getSkuTier()).isEqualTo("some-tier");
    }

    @Test
    public void testStorageAutoGrow() {
        when(server.getParameter(STORAGE_AUTO_GROW, Boolean.class)).thenReturn(true);
        assertThat(underTest.getStorageAutoGrow()).isEqualTo(true);
    }

    @Test
    public void testAllocatedStorageInMB() {
        when(server.getStorageSize()).thenReturn(5L);
        assertThat(underTest.getAllocatedStorageInMb()).isEqualTo(5L * 1024);
    }

    @Test
    public void testDbServerName() {
        when(server.getServerId()).thenReturn("some-name");
        assertThat(underTest.getDbServerName()).isEqualTo("some-name");
    }

    @Test
    public void testDatabaseType() {
        when(server.getEngine()).thenReturn(DatabaseEngine.POSTGRESQL);
        assertThat(underTest.getDatabaseType()).isEqualTo("postgres");
    }

    @Test
    public void testAdminLoginName() {
        when(server.getRootUserName()).thenReturn("some-user");
        assertThat(underTest.getAdminLoginName()).isEqualTo("some-user");
    }

    @Test
    public void testAdminPassword() {
        when(server.getRootPassword()).thenReturn("some-password");
        assertThat(underTest.getAdminPassword()).isEqualTo("some-password");
    }
}
