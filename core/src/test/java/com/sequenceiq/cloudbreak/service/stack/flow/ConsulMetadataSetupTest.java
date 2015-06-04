package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.doNothing;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.StackService;


public class ConsulMetadataSetupTest {

    private static final String DUMMY_AMBARI_ADDRESS = "52.51.50.49";
    private static final String DUMMY_ADDRESS = "52.51.50.48";
    private static final String INSTANCE_GROUP_1 = "john1.john.j5.internal.cloudapp.net";
    private static final String INSTANCE_GROUP_2 = "john2.john.j5.internal.cloudapp.net";

    @Spy
    @InjectMocks
    private ConsulMetadataSetup underTest;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private PollingService<ConsulContext> consulPollingService;

    @Mock
    private ConsulHostCheckerTask consulHostCheckerTask;

    private Stack stack;

    @Before
    public void setUp() {
        underTest = new ConsulMetadataSetup();
        MockitoAnnotations.initMocks(this);
        stack = createStack();
    }

    @Test(expected = WrongMetadataException.class)
    public void testSetupConsulMetadataWhenCannotConnectConsul() {
        // GIVEN
        stack.getInstanceGroupByInstanceGroupName(INSTANCE_GROUP_1)
                .setInstanceMetaData(createInstanceMetaDataWithAmbariAddress(DUMMY_AMBARI_ADDRESS));
        stack.getInstanceGroupByInstanceGroupName(INSTANCE_GROUP_2)
                .setInstanceMetaData(createInstanceMetaDataWithAmbariAddress(DUMMY_AMBARI_ADDRESS));
        given(stackService.getById(1L)).willReturn(stack);
        given(consulPollingService.pollWithTimeout(any(ConsulServiceCheckerTask.class), any(ConsulContext.class),
                anyInt(), anyInt())).willReturn(PollingResult.TIMEOUT).willReturn(PollingResult.EXIT);
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        ConsulClientConfig clientConfig = new ConsulClientConfig(gatewayInstance.getPublicIp(), stack.getCertDir());
        doNothing().when(underTest).updateWithConsulData(clientConfig, anySet(), anySet());
        // WHEN
        underTest.setupConsulMetadata(1L);
    }

    private Set<InstanceMetaData> createInstanceMetaDataWithAmbariAddress(String ambariPublicIp) {
        Set<InstanceMetaData> metaData = new HashSet<>();
        Set<InstanceGroup> instanceGroups = createInstanceGroups();
        InstanceMetaData imd1 = new InstanceMetaData();
        imd1.setPrivateIp(DUMMY_AMBARI_ADDRESS);
        imd1.setPublicIp(ambariPublicIp);
        imd1.setInstanceGroup(instanceGroups.iterator().next());
        InstanceMetaData imd2 = new InstanceMetaData();
        imd2.setInstanceGroup(instanceGroups.iterator().next());
        imd2.setPublicIp(DUMMY_ADDRESS);
        metaData.add(imd1);
        metaData.add(imd2);
        return metaData;
    }

    private Set<InstanceGroup> createInstanceGroups() {
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        InstanceGroup ig1 = new InstanceGroup();
        ig1.setNodeCount(1);
        ig1.setGroupName(INSTANCE_GROUP_1);
        InstanceGroup ig2 = new InstanceGroup();
        ig2.setNodeCount(1);
        ig2.setGroupName(INSTANCE_GROUP_2);
        instanceGroups.add(ig1);
        instanceGroups.add(ig2);
        return instanceGroups;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setInstanceGroups(createInstanceGroups());
        stack.setId(1L);
        return stack;
    }
}
