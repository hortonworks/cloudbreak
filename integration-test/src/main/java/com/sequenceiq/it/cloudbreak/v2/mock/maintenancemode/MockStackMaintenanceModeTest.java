package com.sequenceiq.it.cloudbreak.v2.mock.maintenancemode;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.MAINTENANCE_MODE_ENABLED;
import static org.testng.AssertJUnit.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.MaintenanceModeStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.repository.RepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;

public class MockStackMaintenanceModeTest extends AbstractCloudbreakIntegrationTest {

    private String stackName;

    private Long workspaceId;

    @BeforeClass
    @Parameters({"mockPort", "sshPort"})
    public void configMockServer(@Optional("9443") int mockPort, @Optional("2020") int sshPort) {
        IntegrationTestContext itContext = getItContext();
        Map<String, InstanceGroupV4Request> instanceGroupV2RequestMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);
        int numberOfServers = 0;
        for (InstanceGroupV4Request igr : instanceGroupV2RequestMap.values()) {
            numberOfServers += igr.getNodeCount();
        }

        stackName = itContext.getContextParam(CloudbreakV2Constants.STACK_NAME);
        workspaceId = itContext.getContextParam(CloudbreakV2Constants.WORKSPACE_ID, Long.class);

        MaintenanceModeMock maintenanceModeMock = applicationContext.getBean(MaintenanceModeMock.class, mockPort, sshPort, numberOfServers);
        maintenanceModeMock.addSPIEndpoints();
        maintenanceModeMock.addAmbariMappings(stackName);
        itContext.putContextParam(CloudbreakV2Constants.MOCK_SERVER, maintenanceModeMock);
        itContext.putContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, maintenanceModeMock.getInstanceMap());
    }

    @Test(priority = 1)
    public void testEnableMaintenanceMode() {
        getItContext().putContextParam(CloudbreakITContextConstants.WORKSPACE_ID, workspaceId);

        postMaintenanceMode(workspaceId, MaintenanceModeStatus.ENABLED);

        String stackName = getItContext().getContextParam(CloudbreakITContextConstants.STACK_NAME);
        checkDesiredStatuses(workspaceId, stackName, MAINTENANCE_MODE_ENABLED);
    }

    private void checkDesiredStatuses(Long workspaceId, String stackName, Status desiredClusterStatus) {
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", AVAILABLE.name());
        desiredStatuses.put("clusterStatus", desiredClusterStatus.name());
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), workspaceId, stackName, desiredStatuses);
    }

    private void postMaintenanceMode(Long workspaceId, MaintenanceModeStatus maintenanceModeStatus) {
        MaintenanceModeV4Request maintenanceMode = new MaintenanceModeV4Request();
        maintenanceMode.setStatus(maintenanceModeStatus);
        getCloudbreakClient().stackV4Endpoint().setClusterMaintenanceMode(workspaceId, stackName, maintenanceMode);
    }

    @Test(priority = 2)
    public void testUpdateAmbariDetailsInMaintenanceMode() {
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        UpdateClusterV4Request udateJson = new UpdateClusterV4Request();
        var stackRepository = new StackRepositoryV4Request();
        stackRepository.setStack("AMBARI");
        stackRepository.setVersion("2.7");
        var repoRequest = new RepositoryV4Request();
        repoRequest.setBaseUrl("http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.7.0.0");
        repoRequest.setGpgKeyUrl("http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.7.0.0/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins");
        stackRepository.setRepository(repoRequest);
        udateJson.setStackRepository(stackRepository);

        getCloudbreakClient().stackV4Endpoint().putCluster(workspaceId, stackName, udateJson);

        var stackResponse = getCloudbreakClient().stackV4Endpoint().get(workspaceId, stackName, new HashSet<>());
        assertEquals("Ambari", stackRepository.getVersion(), stackResponse.getCluster().getAmbari().getStackRepository().getVersion());
        assertEquals("Ambari", stackRepository.getRepository().getBaseUrl(),
                stackResponse.getCluster().getAmbari().getStackRepository().getRepository().getBaseUrl());
        assertEquals("Ambari", stackRepository.getRepository().getGpgKeyUrl(),
                stackResponse.getCluster().getAmbari().getStackRepository().getRepository().getGpgKeyUrl());
    }

    @Test(priority = 3)
    public void testUpdateHDPDetailsInMaintenanceMode() {
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        UpdateClusterV4Request udateJson = new UpdateClusterV4Request();
        var stackRepository = new StackRepositoryV4Request();
        stackRepository.setStack("HDP");
        stackRepository.setVersion("2.6");
        stackRepository.setEnableGplRepo(false);
        stackRepository.setMpacks(List.of());
        stackRepository.setOsType("redhat7");
//        var repo = new RepositoryV4Request();
//        repo.setVersion("2.6.5.0");
//        repo.setBaseUrl("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0");
        stackRepository.setRepoId("HDP-2.6");
        stackRepository.setUtilsBaseURL("http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.22/repos/centos7");
        stackRepository.setUtilsRepoId("HDP-UTILS-1.1.0.22");
        stackRepository.setVersion("2.6");
        stackRepository.setVersionDefinitionFileUrl("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0/HDP-2.6.5.0-292.xml");
        udateJson.setStackRepository(stackRepository);

        getCloudbreakClient().stackV4Endpoint().putCluster(workspaceId, stackName, udateJson);

        var stackResponse = getCloudbreakClient().stackV4Endpoint().get(workspaceId, stackName, new HashSet<>());
        var stackDetailsResponse = stackResponse.getCluster().getAmbari().getStackRepository();
        assertEquals("HDP", stackRepository.getVersion(), stackDetailsResponse.getRepoId());
        assertEquals("HDP", stackRepository.getVersionDefinitionFileUrl(),
                stackResponse.getCluster().getAmbari().getStackRepository().getVersionDefinitionFileUrl());
    }

    @Test(priority = 4)
    public void testAttemptUpscaleInMaintenanceMode() {
        var workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        var updateRequest = new StackScaleV4Request();
        var stackId = Long.parseLong(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID));
        var stackName = getItContext().getContextParam(CloudbreakITContextConstants.STACK_NAME);
        updateRequest.setStackId(stackId);
        updateRequest.setGroup("worker");
        updateRequest.setDesiredCount(5);
        getCloudbreakClient().stackV4Endpoint().putScaling(workspaceId, stackName, updateRequest);

        checkDesiredStatuses(workspaceId, stackName, MAINTENANCE_MODE_ENABLED);
    }

    @Test(priority = 5)
    public void testAttemptStopInMaintenanceMode() {
        var workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        getCloudbreakClient().stackV4Endpoint().putStop(workspaceId, stackName);

        var stackName = getItContext().getContextParam(CloudbreakITContextConstants.STACK_NAME);
        checkDesiredStatuses(workspaceId, stackName, MAINTENANCE_MODE_ENABLED);
    }

    @Test(priority = 6)
    public void testRequestValidationForMaintenanceMode() {
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);

        postMaintenanceMode(workspaceId, MaintenanceModeStatus.VALIDATION_REQUESTED);

        String stackName = getItContext().getContextParam(CloudbreakITContextConstants.STACK_NAME);
        checkDesiredStatuses(workspaceId, stackName, MAINTENANCE_MODE_ENABLED);

        MaintenanceModeMock maintenanceModeMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, MaintenanceModeMock.class);
        maintenanceModeMock.verify();
    }

    @Test(priority = 7)
    public void testDisableMaintenanceMode() {
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String stackName = getItContext().getContextParam(CloudbreakITContextConstants.STACK_NAME);

        postMaintenanceMode(workspaceId, MaintenanceModeStatus.DISABLED);

        checkDesiredStatuses(workspaceId, stackName, AVAILABLE);
    }

    @AfterClass
    public void breakDown() {
        MaintenanceModeMock maintenanceModeMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, MaintenanceModeMock.class);
        maintenanceModeMock.stop();
    }
}
