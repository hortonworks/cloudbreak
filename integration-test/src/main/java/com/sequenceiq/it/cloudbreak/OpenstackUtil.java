package com.sequenceiq.it.cloudbreak;

import java.util.List;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.it.IntegrationTestContext;



public class OpenstackUtil extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualRecoveryTest.class);

    private static String instanceId;

    private static ServerApi serverApi;

    private static String stackName;

    @Value("${integrationtest.openstackcredential.tenantName}")
    private String defaultTenantName;

    @Value("${integrationtest.openstackcredential.userName}")
    private String defaultUserName;

    @Value("${integrationtest.openstackcredential.password}")
    private String defaultPassword;

    @Value("${integrationtest.openstackcredential.endpoint}")
    private String defaultEndpoint;

    @Test
    @Parameters({ "osRegion", "hostGroup", "tenantName", "userName", "password", "endpoint"})
    public void openstackUtils(@Optional("RegionOne") String osRegion, String hostGroup, @Optional("") String tenantName,
            @Optional("") String userName, @Optional("") String password, @Optional("") String endpoint) throws Exception {
        //GIVEN
        tenantName = StringUtils.hasLength(tenantName) ? tenantName : defaultTenantName;
        userName = StringUtils.hasLength(userName) ? userName : defaultUserName;
        password = StringUtils.hasLength(password) ? password : defaultPassword;
        endpoint = StringUtils.hasLength(endpoint) ? endpoint : defaultEndpoint;

        String provider = "openstack-nova";

        NovaApi novaApi = ContextBuilder.newBuilder(provider)
                .endpoint(endpoint)
                .credentials(tenantName + ":" + userName, password)
                .buildApi(NovaApi.class);

        serverApi = novaApi.getServerApi(osRegion);
        getId(hostGroup);
        checkInstanceIdWithOs(hostGroup);
        //WHEN
        deleteInstance();
        //THEN
        checkInstanceIsDeleted();
    }

    private void getId(String hostGroup) {
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);

        StackEndpoint stackEndpoint = getCloudbreakClient().stackEndpoint();
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));
        List<InstanceGroupResponse> instanceGroups = stackResponse.getInstanceGroups();
        stackName = stackResponse.getName();
        LOGGER.info(stackName);

        outerloop:
        for (InstanceGroupResponse instanceGroup : instanceGroups) {
            if (hostGroup.equals(instanceGroup.getGroup().toString())) {
                Set<InstanceMetaDataJson> instanceMetaData = instanceGroup.getMetadata();
                for (InstanceMetaDataJson metaData : instanceMetaData) {
                    instanceId = metaData.getInstanceId();
                    break outerloop;
                }
            }
        }
        Assert.assertNotNull(instanceId);
    }

    private void checkInstanceIdWithOs(String hostGroup) {
        for (Server server : serverApi.listInDetail().concat()) {
            if (server.getId().equals(instanceId)) {
                Assert.assertEquals(server.getMetadata().get("cb_instance_group_name"), hostGroup);
                Assert.assertTrue(server.getName().contains(stackName));
                break;
                }
            }
    }

    private void checkInstanceIsDeleted() {
        Boolean instanceExisted = Boolean.FALSE;

        for (Server server : serverApi.listInDetail().concat()) {
            if (server.getId().equals(instanceId)) {
                instanceExisted = Boolean.TRUE;
                break;
            }
        }

        if (instanceExisted == Boolean.FALSE) {
            return;
        }
        for (int i = 0; i < 30; i++) {
            LOGGER.info("Check instance has deleted with Openstack api ...");
            instanceExisted = Boolean.FALSE;
            for (Server server : serverApi.listInDetail().concat()) {
                if (server.getId().equals(instanceId)) {
                    instanceExisted = Boolean.TRUE;
                    break;
                }
            }
            if (instanceExisted == Boolean.TRUE) {
                continue;
            } else {
                break;
            }
        }
        Assert.assertFalse(instanceExisted);
    }

    private void deleteInstance() {
        LOGGER.info("Deleting instance with id: {}", instanceId);
        serverApi.delete(instanceId);
    }
}
