package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.google.api.services.monitoring.v3.model.Point;
import com.google.api.services.monitoring.v3.model.TimeInterval;
import com.google.api.services.monitoring.v3.model.TimeSeries;
import com.google.api.services.monitoring.v3.model.TypedValue;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.api.services.sqladmin.model.Settings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpMonitoringFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerStorageMetrics;

@ExtendWith(MockitoExtension.class)
class GcpDatabaseServerMetricServiceTest {

    private static final String DB_SERVER_NAME = "test";

    private static final String PROJECT_ID = "project-id";

    private static final long BYTES_IN_GB = 1024L * 1024L * 1024L;

    @Mock
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Mock
    private GcpMonitoringFactory gcpMonitoringFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DatabaseServer databaseServer;

    @InjectMocks
    private GcpDatabaseServerMetricService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(ac.getCloudCredential()).thenReturn(cloudCredential);
        lenient().when(cloudCredential.getName()).thenReturn("credential");
        lenient().when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        lenient().when(databaseServer.getServerId()).thenReturn(DB_SERVER_NAME);
        lenient().when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn(PROJECT_ID);
    }

    private void stubDatabaseInstance(Long dataDiskSizeGb) throws IOException {
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances instances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List list = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse listResponse = mock(InstancesListResponse.class);
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(sqlAdmin.instances()).thenReturn(instances);
        when(instances.list(anyString())).thenReturn(list);
        when(list.execute()).thenReturn(listResponse);
        when(listResponse.isEmpty()).thenReturn(false);
        DatabaseInstance databaseInstance = new DatabaseInstance();
        databaseInstance.setName(DB_SERVER_NAME);
        Settings settings = new Settings();
        settings.setDataDiskSizeGb(dataDiskSizeGb);
        databaseInstance.setSettings(settings);
        when(listResponse.getItems()).thenReturn(List.of(databaseInstance));
    }

    private void stubEmptyInstanceList() throws IOException {
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances instances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.List list = mock(SQLAdmin.Instances.List.class);
        InstancesListResponse listResponse = mock(InstancesListResponse.class);
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(sqlAdmin.instances()).thenReturn(instances);
        when(instances.list(anyString())).thenReturn(list);
        when(list.execute()).thenReturn(listResponse);
        when(listResponse.isEmpty()).thenReturn(true);
    }

    private void stubMonitoring(ListTimeSeriesResponse response) throws IOException {
        Monitoring monitoring = mock(Monitoring.class);
        Monitoring.Projects projects = mock(Monitoring.Projects.class);
        Monitoring.Projects.TimeSeries timeSeries = mock(Monitoring.Projects.TimeSeries.class);
        Monitoring.Projects.TimeSeries.List list = mock(Monitoring.Projects.TimeSeries.List.class);
        when(gcpMonitoringFactory.buildMonitoring(any(CloudCredential.class), anyString())).thenReturn(monitoring);
        when(monitoring.projects()).thenReturn(projects);
        when(projects.timeSeries()).thenReturn(timeSeries);
        when(timeSeries.list(anyString())).thenReturn(list);
        when(list.setFilter(anyString())).thenReturn(list);
        when(list.setIntervalStartTime(anyString())).thenReturn(list);
        when(list.setIntervalEndTime(anyString())).thenReturn(list);
        when(list.setAggregationPerSeriesAligner(anyString())).thenReturn(list);
        when(list.setAggregationAlignmentPeriod(anyString())).thenReturn(list);
        when(list.execute()).thenReturn(response);
    }

    private ListTimeSeriesResponse timeSeriesWithUtilization(double utilization, String endTime) {
        TypedValue value = new TypedValue().setDoubleValue(utilization);
        TimeInterval interval = new TimeInterval().setEndTime(endTime);
        Point point = new Point().setValue(value).setInterval(interval);
        TimeSeries series = new TimeSeries().setPoints(List.of(point));
        return new ListTimeSeriesResponse().setTimeSeries(List.of(series));
    }

    @Test
    void shouldComputeFreePercentageFromUtilization() throws IOException {
        stubDatabaseInstance(100L);
        stubMonitoring(timeSeriesWithUtilization(0.95d, "2026-10-03T10:00:00Z"));

        Optional<DatabaseServerStorageMetrics> result = underTest.getDatabaseServerStorageMetrics(ac, databaseStack);

        assertThat(result).isPresent();
        assertThat(result.get().freeStoragePercentage()).isCloseTo(5.0d, within(0.0001d));
        assertThat(result.get().allocatedBytes()).isEqualTo(100L * BYTES_IN_GB);
        assertThat(result.get().freeBytes()).isEqualTo(Math.round(100L * BYTES_IN_GB * 0.05d));
    }

    @Test
    void shouldPickLatestDatapointByEndTime() throws IOException {
        stubDatabaseInstance(100L);
        TypedValue olderValue = new TypedValue().setDoubleValue(0.10d);
        Point older = new Point().setValue(olderValue).setInterval(new TimeInterval().setEndTime("2026-10-03T09:00:00Z"));
        TypedValue newerValue = new TypedValue().setDoubleValue(0.90d);
        Point newer = new Point().setValue(newerValue).setInterval(new TimeInterval().setEndTime("2026-10-03T10:00:00Z"));
        TimeSeries series = new TimeSeries().setPoints(List.of(older, newer));
        stubMonitoring(new ListTimeSeriesResponse().setTimeSeries(List.of(series)));

        Optional<DatabaseServerStorageMetrics> result = underTest.getDatabaseServerStorageMetrics(ac, databaseStack);

        assertThat(result).isPresent();
        assertThat(result.get().freeStoragePercentage()).isCloseTo(10.0d, within(0.0001d));
    }

    @Test
    void shouldReturnEmptyWhenInstanceNotFound() throws IOException {
        stubEmptyInstanceList();

        assertThat(underTest.getDatabaseServerStorageMetrics(ac, databaseStack)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoTimeSeries() throws IOException {
        stubDatabaseInstance(100L);
        stubMonitoring(new ListTimeSeriesResponse());

        assertThat(underTest.getDatabaseServerStorageMetrics(ac, databaseStack)).isEmpty();
    }

    @Test
    void shouldReturnNullFreeBytesWhenAllocatedUnknown() throws IOException {
        stubDatabaseInstance(null);
        stubMonitoring(timeSeriesWithUtilization(0.80d, "2026-10-03T10:00:00Z"));

        Optional<DatabaseServerStorageMetrics> result = underTest.getDatabaseServerStorageMetrics(ac, databaseStack);

        assertThat(result).isPresent();
        assertThat(result.get().freeStoragePercentage()).isCloseTo(20.0d, within(0.0001d));
        assertThat(result.get().allocatedBytes()).isNull();
        assertThat(result.get().freeBytes()).isNull();
    }
}
