package com.sequenceiq.cloudbreak.service.publicendpoint.dns;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiConfigProvider;
import com.sequenceiq.cloudbreak.dns.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class BaseDnsEntryServiceTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String NIFI_NODE = "NIFI_NODE";

    private static final String FQDN_1 = "fqdn1";

    private static final String FQDN_2 = "fqdn2";

    private static final String FQDN_3 = "fqdn3";

    private static final String PUBLIC_IP_1 = "publicIp1";

    private static final String PUBLIC_IP_2 = "publicIp2";

    private static final String PUBLIC_IP_3 = "publicIp3";

    private static final String ENVIRONMENT_NAME = "envName";

    @Mock
    private DnsManagementService dnsManagementService;

    @Mock
    private EnvironmentBasedDomainNameProvider domainNameProvider;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private CertificateCreationService certificateCreationService;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private ComponentLocatorService componentLocatorService;

    @Spy
    private NifiConfigProvider nifiConfigProvider = new NifiConfigProvider();

    @InjectMocks
    private NifiHostPublicDnsEntryService underTest;

    @BeforeAll
    static void setupAll() {
        ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);
    }

    @BeforeEach
    void setupEach() {
        underTest.setCertGenerationEnabled(true);
    }

    @Test
    void testGetComponentLocation() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);

        underTest.getComponentLocation(stack);

        verify(componentLocatorService).getComponentLocation(cluster, nifiConfigProvider.getRoleTypes());
    }

    @Test
    void testCreateOrUpdate() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroups(stack);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);

        Map<String, List<String>> componentLocation = Map.of(NIFI_NODE, List.of(FQDN_1, FQDN_2, FQDN_3));

        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(componentLocatorService.getComponentLocation(stack.getCluster(), nifiConfigProvider.getRoleTypes()))
                .thenReturn(componentLocation);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(USER_CRN, ACCOUNT_ID, FQDN_1, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_1))).thenReturn(true);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(USER_CRN, ACCOUNT_ID, FQDN_2, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_2))).thenReturn(true);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(USER_CRN, ACCOUNT_ID, FQDN_3, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_3))).thenReturn(true);

        Map<String, String> result = underTest.createOrUpdate(stack);

        assertEquals(3, result.size());
        assertTrue(result.containsKey(FQDN_1));
        assertTrue(result.containsKey(FQDN_2));
        assertTrue(result.containsKey(FQDN_3));
        assertEquals(PUBLIC_IP_1, result.get(FQDN_1));
        assertEquals(PUBLIC_IP_2, result.get(FQDN_2));
        assertEquals(PUBLIC_IP_3, result.get(FQDN_3));
    }

    @Test
    void testCreateOrUpdateCandidates() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroups(stack);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);

        Map<String, List<String>> componentLocation = Map.of(NIFI_NODE, List.of(FQDN_1, FQDN_2, FQDN_3));

        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(componentLocatorService.getComponentLocation(stack.getCluster(), nifiConfigProvider.getRoleTypes()))
                .thenReturn(componentLocation);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(USER_CRN, ACCOUNT_ID, FQDN_1, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_1))).thenReturn(true);

        Map<String, String> result = underTest.createOrUpdateCandidates(stack, Map.of(FQDN_1, PUBLIC_IP_1));

        assertEquals(1, result.size());
        assertTrue(result.containsKey(FQDN_1));
        assertEquals(PUBLIC_IP_1, result.get(FQDN_1));
    }

    @Test
    void testDeregister() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroups(stack);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);

        Map<String, List<String>> componentLocation = Map.of(NIFI_NODE, List.of(FQDN_1, FQDN_2, FQDN_3));

        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(componentLocatorService.getComponentLocation(stack.getCluster(), nifiConfigProvider.getRoleTypes()))
                .thenReturn(componentLocation);
        when(dnsManagementService.deleteDnsEntryWithIp(USER_CRN, ACCOUNT_ID, FQDN_1, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_1))).thenReturn(true);
        when(dnsManagementService.deleteDnsEntryWithIp(USER_CRN, ACCOUNT_ID, FQDN_2, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_2))).thenReturn(true);
        when(dnsManagementService.deleteDnsEntryWithIp(USER_CRN, ACCOUNT_ID, FQDN_3, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_3))).thenReturn(true);

        Map<String, String> result = underTest.deregister(stack);

        assertEquals(3, result.size());
        assertTrue(result.containsKey(FQDN_1));
        assertTrue(result.containsKey(FQDN_2));
        assertTrue(result.containsKey(FQDN_3));
        assertEquals(PUBLIC_IP_1, result.get(FQDN_1));
        assertEquals(PUBLIC_IP_2, result.get(FQDN_2));
        assertEquals(PUBLIC_IP_3, result.get(FQDN_3));
    }

    @Test
    void testDeregisterWithCandidates() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        setInstanceGroups(stack);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setName(ENVIRONMENT_NAME);

        Map<String, List<String>> componentLocation = Map.of(NIFI_NODE, List.of(FQDN_1, FQDN_2, FQDN_3));

        when(environmentClientService.getByCrn(stack.getEnvironmentCrn())).thenReturn(environmentResponse);
        when(componentLocatorService.getComponentLocation(stack.getCluster(), nifiConfigProvider.getRoleTypes()))
                .thenReturn(componentLocation);
        when(dnsManagementService.deleteDnsEntryWithIp(USER_CRN, ACCOUNT_ID, FQDN_1, ENVIRONMENT_NAME, false, List.of(PUBLIC_IP_1))).thenReturn(true);

        Map<String, String> result = underTest.deregister(stack, Map.of(FQDN_1, PUBLIC_IP_1));

        assertEquals(1, result.size());
        assertTrue(result.containsKey(FQDN_1));
        assertEquals(PUBLIC_IP_1, result.get(FQDN_1));
    }

    private void setInstanceGroups(Stack stack) {
        InstanceGroup ig1 = getInstanceGroup1();
        InstanceGroup ig2 = getInstanceGroup2();
        InstanceGroup ig3 = getInstanceGroup3();

        Set<InstanceGroup> instanceGroups = Set.of(ig1, ig2, ig3);
        stack.setInstanceGroups(instanceGroups);
    }

    private InstanceGroup getInstanceGroup1() {
        InstanceGroup ig1 = new InstanceGroup();
        ig1.setInstanceGroupType(InstanceGroupType.CORE);
        InstanceMetaData imd1 = new InstanceMetaData();
        imd1.setPrivateId(1L);
        imd1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd1.setDiscoveryFQDN(FQDN_1);
        imd1.setPublicIp(PUBLIC_IP_1);
        InstanceMetaData imd2 = new InstanceMetaData();
        imd2.setPrivateId(2L);
        imd2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd2.setDiscoveryFQDN(FQDN_2);
        imd2.setPublicIp(PUBLIC_IP_2);
        ig1.setInstanceMetaData(Set.of(imd1, imd2));
        return ig1;
    }

    private InstanceGroup getInstanceGroup2() {
        InstanceGroup ig2 = new InstanceGroup();
        ig2.setInstanceGroupType(InstanceGroupType.CORE);
        InstanceMetaData imd3 = new InstanceMetaData();
        imd3.setPrivateId(3L);
        imd3.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd3.setDiscoveryFQDN(FQDN_3);
        imd3.setPublicIp(PUBLIC_IP_3);
        ig2.setInstanceMetaData(Set.of(imd3));
        return ig2;
    }

    private InstanceGroup getInstanceGroup3() {
        InstanceGroup ig3 = new InstanceGroup();
        ig3.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData imd4 = new InstanceMetaData();
        imd4.setPrivateId(4L);
        imd4.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        imd4.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd4.setDiscoveryFQDN("fqdn4");
        InstanceMetaData imd5 = new InstanceMetaData();
        imd5.setPrivateId(5L);
        imd5.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        imd5.setDiscoveryFQDN("fqdn5");
        ig3.setInstanceMetaData(Set.of(imd4, imd5));
        return ig3;
    }

}