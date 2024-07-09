package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Subnetworks;
import com.google.api.services.compute.model.Subnetwork;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.SQLAdmin.Instances;
import com.google.api.services.sqladmin.SQLAdmin.Instances.Get;
import com.google.api.services.sqladmin.SQLAdmin.Instances.Insert;
import com.google.api.services.sqladmin.SQLAdmin.Users;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.api.services.sqladmin.model.IpMapping;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.Settings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpDatabaseServerLaunchServiceTest {

    @Mock
    private DatabasePollerService databasePollerService;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private GcpDatabaseServerUserService gcpDatabaseServerUserService;

    @Captor
    private ArgumentCaptor<DatabaseInstance> databaseInstanceArgumentCaptor;

    @InjectMocks
    private GcpDatabaseServerLaunchService underTest;

    @Test
    public void testGetRdsPort() {
        CloudResource rdsPort = underTest.getRdsPort("az1");

        assertEquals(ResourceType.RDS_PORT, rdsPort.getType());
        assertEquals("5432", rdsPort.getName());
    }

    @Test
    public void testGetRdsHostName() {
        DatabaseInstance databaseInstance = mock(DatabaseInstance.class);
        IpMapping ipMapping = new IpMapping();
        ipMapping.setIpAddress("10.0.0.0");
        ipMapping.setType("PRIVATE");

        when(databaseInstance.getIpAddresses()).thenReturn(List.of(ipMapping));

        CloudResource rdsHostName = underTest.getRdsHostName(databaseInstance, CloudResource.builder(), "id-1", "az1");

        assertEquals(ResourceType.RDS_HOSTNAME, rdsHostName.getType());
        assertEquals("10.0.0.0", rdsHostName.getName());
    }

    @Test
    public void testLaunchWhenDatabaseAlreadyExistShouldNotCreateAgain() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        Compute compute = mock(Compute.class);
        Instances sqlAdminInstances = mock(Instances.class);
        Instances.List sqlAdminInstancesList = mock(Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn("server-1");
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);

        DatabaseInstance databaseInstance = new DatabaseInstance();
        databaseInstance.setName("server-1");
        databaseInstance.setState("RUNNABLE");
        Settings settings = new Settings();
        settings.setActivationPolicy("ALWAYS");
        databaseInstance.setSettings(settings);
        when(instancesListResponse.getItems()).thenReturn(List.of(databaseInstance));
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        List<CloudResource> launch = underTest.launch(authenticatedContext, databaseStack, persistenceNotifier);

        assertEquals(0, launch.size());
    }

    @Test
    public void testLaunchWhenDatabaseNOTAlreadyExistAndSharedProjectIdShouldCreate() throws Exception {
        Network network = new Network(new Subnet("10.0.0.0/16"),
                Map.of(
                        "subnetId", "s-1",
                        "availabilityZone", "a",
                        "sharedProjectId", "sp1"
                ));

        Map<String, Object> map = new HashMap<>();
        map.put("engineVersion", "1");
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withConnectionDriver("driver")
                .withServerId("driver")
                .withConnectorJarUrl("driver")
                .withEngine(DatabaseEngine.POSTGRESQL)
                .withLocation("location")
                .withPort(99)
                .withStorageSize(50L)
                .withRootUserName("rootUserName")
                .withRootPassword("rootPassword")
                .withFlavor("flavor")
                .withUseSslEnforcement(false)
                .withParams(map)
                .build();

        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        Compute compute = mock(Compute.class);
        DatabaseInstance databaseInstance = mock(DatabaseInstance.class);
        Subnetworks subnetworks = mock(Subnetworks.class);
        Subnetworks.Get subnetworksGet = mock(Subnetworks.Get.class);
        Users users = mock(Users.class);
        Users.Insert usersInsert = mock(Users.Insert.class);
        Get instancesGet = mock(Get.class);
        Instances sqlAdminInstances = mock(Instances.class);
        Instances.List sqlAdminInstancesList = mock(Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);
        Operation operation = mock(Operation.class);
        Insert sqlAdminInstancesInsert = mock(Insert.class);
        IpMapping ipMapping = new IpMapping();
        ipMapping.setIpAddress("10.0.0.0");
        ipMapping.setType("PRIVATE");

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseStack.getNetwork()).thenReturn(network);
        when(gcpLabelUtil.createLabelsFromTagsMap(anyMap())).thenReturn(new HashMap<>());
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(compute.subnetworks()).thenReturn(subnetworks);
        when(subnetworks.get(anyString(), anyString(), anyString())).thenReturn(subnetworksGet);
        when(subnetworksGet.execute()).thenReturn(new Subnetwork());
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);
        when(instancesListResponse.getItems()).thenReturn(List.of());
        when(databaseInstance.getName()).thenReturn("name");
        when(sqlAdminInstances.get(anyString(), anyString())).thenReturn(instancesGet);
        when(instancesGet.execute()).thenReturn(databaseInstance);
        when(sqlAdminInstances.insert(anyString(), databaseInstanceArgumentCaptor.capture())).thenReturn(sqlAdminInstancesInsert);
        when(sqlAdminInstancesInsert.setPrettyPrint(anyBoolean())).thenReturn(sqlAdminInstancesInsert);
        when(sqlAdminInstancesInsert.execute()).thenReturn(operation);
        when(databaseInstance.getIpAddresses()).thenReturn(List.of(ipMapping));
        doNothing().when(databasePollerService).launchDatabasePoller(any(AuthenticatedContext.class), anyList());
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        List<CloudResource> launch = underTest.launch(authenticatedContext, databaseStack, persistenceNotifier);

        assertNull(databaseInstanceArgumentCaptor.getValue().getDiskEncryptionConfiguration());

        assertEquals(1, launch.size());
    }

    @Test
    public void testLaunchWhenDatabaseNOTAlreadyExistAndNOTSharedProjectIdShouldCreate() throws Exception {
        Network network = new Network(new Subnet("10.0.0.0/16"),
                Map.of(
                        "subnetId", "s-1",
                        "availabilityZone", "a"
                ));

        Map<String, Object> map = new HashMap<>();
        map.put("engineVersion", "1");
        map.put("key", "value");
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withConnectionDriver("driver")
                .withServerId("driver")
                .withConnectorJarUrl("driver")
                .withEngine(DatabaseEngine.POSTGRESQL)
                .withLocation("location")
                .withPort(99)
                .withStorageSize(50L)
                .withRootUserName("rootUserName")
                .withRootPassword("rootPassword")
                .withFlavor("flavor")
                .withUseSslEnforcement(true)
                .withParams(map)
                .build();

        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        Compute compute = mock(Compute.class);
        DatabaseInstance databaseInstance = mock(DatabaseInstance.class);
        Subnetworks subnetworks = mock(Subnetworks.class);
        Subnetworks.Get subnetworksGet = mock(Subnetworks.Get.class);
        Users users = mock(Users.class);
        Users.Insert usersInsert = mock(Users.Insert.class);
        Get instancesGet = mock(Get.class);
        Instances sqlAdminInstances = mock(Instances.class);
        Instances.List sqlAdminInstancesList = mock(Instances.List.class);
        InstancesListResponse instancesListResponse = mock(InstancesListResponse.class);
        Operation operation = mock(Operation.class);
        Insert sqlAdminInstancesInsert = mock(Insert.class);
        IpMapping ipMapping = new IpMapping();
        ipMapping.setIpAddress("10.0.0.0");
        ipMapping.setType("PRIVATE");

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseStack.getNetwork()).thenReturn(network);
        when(gcpLabelUtil.createLabelsFromTagsMap(anyMap())).thenReturn(new HashMap<>());
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(compute.subnetworks()).thenReturn(subnetworks);
        when(subnetworks.get(anyString(), anyString(), anyString())).thenReturn(subnetworksGet);
        when(subnetworksGet.execute()).thenReturn(new Subnetwork());
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenReturn(instancesListResponse);
        when(instancesListResponse.getItems()).thenReturn(List.of());
        when(databaseInstance.getName()).thenReturn("name");
        when(sqlAdminInstances.get(anyString(), anyString())).thenReturn(instancesGet);
        when(instancesGet.execute()).thenReturn(databaseInstance);
        when(sqlAdminInstances.insert(anyString(), databaseInstanceArgumentCaptor.capture())).thenReturn(sqlAdminInstancesInsert);
        when(sqlAdminInstancesInsert.setPrettyPrint(anyBoolean())).thenReturn(sqlAdminInstancesInsert);
        when(sqlAdminInstancesInsert.execute()).thenReturn(operation);
        when(databaseInstance.getIpAddresses()).thenReturn(List.of(ipMapping));
        doNothing().when(databasePollerService).launchDatabasePoller(any(AuthenticatedContext.class), anyList());
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        doNothing().when(gcpDatabaseServerUserService).createUser(any(), any(), any(), anyString());
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));

        List<CloudResource> launch = underTest.launch(authenticatedContext, databaseStack, persistenceNotifier);

        assertEquals("value", databaseInstanceArgumentCaptor.getValue().getDiskEncryptionConfiguration().getKmsKeyName());

        assertEquals(1, launch.size());
    }

    @Test
    public void testLaunchWhenDatabaseNOTAlreadyExistAndListThrowExceptionShouldThrowGcpResourceException() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("engineVersion", "1");
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withConnectionDriver("driver")
                .withServerId("driver")
                .withConnectorJarUrl("driver")
                .withEngine(DatabaseEngine.POSTGRESQL)
                .withLocation("location")
                .withPort(99)
                .withStorageSize(50L)
                .withRootUserName("rootUserName")
                .withRootPassword("rootPassword")
                .withFlavor("flavor")
                .withUseSslEnforcement(true)
                .withParams(map)
                .build();

        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        Compute compute = mock(Compute.class);
        Instances sqlAdminInstances = mock(Instances.class);
        Instances.List sqlAdminInstancesList = mock(Instances.List.class);
        IpMapping ipMapping = new IpMapping();
        ipMapping.setIpAddress("10.0.0.0");
        ipMapping.setType("PRIVATE");
        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);
        GoogleJsonError googleJsonError = mock(GoogleJsonError.class);
        when(googleJsonResponseException.getDetails()).thenReturn(googleJsonError);
        when(googleJsonResponseException.getMessage()).thenReturn("error");
        when(googleJsonError.getMessage()).thenReturn("error");

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn("credential");
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(gcpSQLAdminFactory.buildSQLAdmin(any(CloudCredential.class), anyString())).thenReturn(sqlAdmin);
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("region"), availabilityZone("az1")));
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.list(anyString())).thenReturn(sqlAdminInstancesList);
        when(sqlAdminInstancesList.execute()).thenThrow(googleJsonResponseException);

        GcpResourceException gcpResourceException = assertThrows(GcpResourceException.class,
                () -> underTest.launch(authenticatedContext, databaseStack, persistenceNotifier));

        assertEquals("error: [ resourceType: GCP_DATABASE,  resourceName: driver ]", gcpResourceException.getMessage());
    }

    @Test
    public void testLaunchWhenDBSSLEnforcementIsRequested() {
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.isUseSslEnforcement()).thenReturn(true);
        GcpDatabaseServerView gcpDatabaseServerView = mock(GcpDatabaseServerView.class);
        Subnetwork subnetwork = mock(Subnetwork.class);

        Settings settings = underTest.getSettings(databaseStack, gcpDatabaseServerView, subnetwork);

        assertEquals("ENCRYPTED_ONLY", settings.getIpConfiguration().getSslMode());
    }

    @Test
    public void testLaunchWhenDBSSLEnforcementIsNOTRequested() {
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.isUseSslEnforcement()).thenReturn(false);
        GcpDatabaseServerView gcpDatabaseServerView = mock(GcpDatabaseServerView.class);
        Subnetwork subnetwork = mock(Subnetwork.class);

        Settings settings = underTest.getSettings(databaseStack, gcpDatabaseServerView, subnetwork);

        assertEquals("ALLOW_UNENCRYPTED_AND_ENCRYPTED", settings.getIpConfiguration().getSslMode());
    }
}
