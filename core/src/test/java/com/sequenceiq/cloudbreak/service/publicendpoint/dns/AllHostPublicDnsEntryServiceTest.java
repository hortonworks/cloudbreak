package com.sequenceiq.cloudbreak.service.publicendpoint.dns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class AllHostPublicDnsEntryServiceTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String ENVIRONMENT_NAME = "envName";

    private static final String FQDN = "fqdn";

    private static final String PUBLIC_IP = "publicIp";

    private static final String ECS_MASTER = "ecs_master";

    private static final String MASTER = "master";

    private static final String WORKER = "worker";

    private static final String CONSOLE_CDP_APPS = "console-cdp.apps";

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private DnsManagementService dnsManagementService;

    @InjectMocks
    private AllHostPublicDnsEntryService underTest;

    @Test
    void getComponentLocationWhenPrimaryGatewayIsTheOnlyNodeWithinGroup() {
        Stack stack = TestUtil.stack();

        Map<String, List<String>> result = underTest.getComponentLocation(stack);

        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        Assertions.assertFalse(result.containsKey(primaryGatewayInstance.getInstanceGroupName()), "Result should not contain primary gateway's group name");
        Assertions.assertFalse(resultContainsInstanceMetadata(primaryGatewayInstance, result), "Result should not contain primary gateway's instance metadata");
        Assertions.assertEquals(2, result.size(), "Result should contain all non primary gateway instance group");
    }

    @Test
    void getComponentLocationWhenPrimaryGatewayIsNotTheOnlyNodeWithinGatewayHostGroup() {
        Stack stack = TestUtil.stack();
        InstanceMetaData primaryGatewayInstance = (InstanceMetaData) stack.getPrimaryGatewayInstance();
        InstanceGroup gatewayInstanceGroup = primaryGatewayInstance.getInstanceGroup();
        InstanceMetaData otherGatewayInstanceMetadata = new InstanceMetaData();
        otherGatewayInstanceMetadata.setDiscoveryFQDN("something.new");
        otherGatewayInstanceMetadata.setInstanceGroup(gatewayInstanceGroup);
        otherGatewayInstanceMetadata.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        Set<InstanceMetaData> updatedIM = gatewayInstanceGroup.getNotTerminatedAndNotZombieInstanceMetaDataSet();
        updatedIM.add(otherGatewayInstanceMetadata);
        gatewayInstanceGroup.replaceInstanceMetadata(updatedIM);


        Map<String, List<String>> result = underTest.getComponentLocation(stack);

        Assertions.assertTrue(result.containsKey(primaryGatewayInstance.getInstanceGroupName()), "Result should contain primary gateway's group name");
        Assertions.assertTrue(resultContainsInstanceMetadata(otherGatewayInstanceMetadata, result), "Result should contain other gateway's instance metadata");
        Assertions.assertFalse(resultContainsInstanceMetadata(primaryGatewayInstance, result), "Result should not contain primary gateway's instance metadata");
        Assertions.assertEquals(3, result.size(), "Result should contain all instance group");
    }

    @Test
    void getComponentLocationWhenPrimaryGatewayDoesNotExist() {
        Stack stack = new Stack();

        Map<String, List<String>> result = underTest.getComponentLocation(stack);

        Assertions.assertTrue(result.isEmpty(), "Result couldn't contain any hostname as primary gateway does not exist");
    }

    @Test
    void getComponentLocationWhenNodesDoesNotHaveDiscoveryFQDN() {
        Stack stack = TestUtil.stack();
        stack.getNotTerminatedAndNotZombieInstanceMetaDataList()
                .forEach(im -> im.setDiscoveryFQDN(null));

        Map<String, List<String>> result = underTest.getComponentLocation(stack);

        Assertions.assertTrue(result.isEmpty(), "Result couldn't contain any hostname as nodes do not have discovery FQDN");
    }

    @Test
    void getComponentLocationWhenTheGatewayNodeHaveOnlyDiscoveryFQDN() {
        Stack stack = TestUtil.stack();
        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        stack.getNotTerminatedAndNotZombieInstanceMetaDataList().stream()
                .filter(im -> !primaryGatewayInstance.getId().equals(im.getId()))
                .forEach(im -> im.setDiscoveryFQDN(null));

        Map<String, List<String>> result = underTest.getComponentLocation(stack);

        Assertions.assertTrue(result.isEmpty(), "Result couldn't contain any hostname as nodes do not have discovery FQDN except gateway");
    }

    @ParameterizedTest
    @CsvSource({"1", "2", "3"})
    void testCreateOrUpdateHybridOnPrem(int ecsMasterNodeCount) throws PemDnsEntryCreateOrUpdateException {
        underTest.setCertGenerationEnabled(true);
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroupsHybrid(stack, ecsMasterNodeCount);

        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);
        environmentResponse.setEnvironmentType(EnvironmentType.HYBRID.toString());

        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);

        Map<String, String> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createOrUpdate(stack));

        assertEquals(4 + ecsMasterNodeCount, result.size());

        assertEquals(PUBLIC_IP + ECS_MASTER + 0, result.get(FQDN + ECS_MASTER + 0));
        assertEquals(PUBLIC_IP + WORKER + 0, result.get(FQDN + WORKER + 0));
        assertEquals(PUBLIC_IP + WORKER + 1, result.get(FQDN + WORKER + 1));
        assertEquals(PUBLIC_IP + WORKER + 2, result.get(FQDN + WORKER + 2));
        assertTrue(getEcsMasterIpList(ecsMasterNodeCount).contains(result.get(CONSOLE_CDP_APPS)));

        String consoleCdpAppsIp = result.get(CONSOLE_CDP_APPS);
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(ACCOUNT_ID, CONSOLE_CDP_APPS, ENVIRONMENT_NAME, false, List.of(consoleCdpAppsIp));
    }

    private List<String> getEcsMasterIpList(int ecsMasterNodeCount) {
        ArrayList ecsMasterIps = new ArrayList<String>();
        for (int i = 0; i < ecsMasterNodeCount; i++) {
            ecsMasterIps.add(PUBLIC_IP + ECS_MASTER + i);
        }
        return ecsMasterIps;
    }

    private void setInstanceGroupsHybrid(Stack stack, int ecsMasterNodeCount) {
        InstanceGroup ig1 = getInstanceGroup(ECS_MASTER, InstanceGroupType.CORE, ecsMasterNodeCount);
        InstanceGroup ig2 = getInstanceGroup(MASTER, InstanceGroupType.GATEWAY, 1);
        InstanceGroup ig3 = getInstanceGroup(WORKER, InstanceGroupType.CORE, 3);

        Set<InstanceGroup> instanceGroups = Set.of(ig1, ig2, ig3);
        stack.setInstanceGroups(instanceGroups);

    }

    private InstanceGroup getInstanceGroup(String igName, InstanceGroupType igType, int nodeCount) {
        InstanceGroup ig = new InstanceGroup();
        ig.setInstanceGroupType(igType);
        ig.setInstanceMetaData(new HashSet<>());
        ig.setGroupName(igName);
        for (int i = 0; i < nodeCount; i++) {
            InstanceMetaData imd = new InstanceMetaData();
            imd.setInstanceMetadataType(igType == InstanceGroupType.GATEWAY ? InstanceMetadataType.GATEWAY_PRIMARY : InstanceMetadataType.CORE);
            imd.setPrivateId(RandomUtils.nextLong());
            imd.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            imd.setDiscoveryFQDN(FQDN + igName + i);
            imd.setPublicIp(PUBLIC_IP + igName + i);
            imd.setInstanceGroup(ig);
            ig.getInstanceMetaData().add(imd);
        }
        return ig;
    }

    private boolean resultContainsInstanceMetadata(InstanceMetadataView primaryGatewayInstance, Map<String, List<String>> result) {
        return result
                .values()
                .stream()
                .anyMatch(ims -> ims.stream().anyMatch(im -> im.equals(primaryGatewayInstance.getDiscoveryFQDN())));
    }
}