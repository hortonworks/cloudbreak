package com.sequenceiq.cloudbreak.cm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerDecomissionerTest {

    private static final String STACK_NAME = "stack";

    private static final String DELETED_INSTANCE_FQD = "deletedInstance";

    private static final String RUNNING_INSTANCE_FQDN = "runningInstance";

    @InjectMocks
    private ClouderaManagerDecomissioner underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient client;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Test
    void collectHostsToRemoveShouldCollectDeletedOnProviderSideNodes() throws Exception {
        HostGroup hostGroup = new HostGroup();
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData deletedInstanceMetaData = createInstanceMetadata(InstanceStatus.DELETED_ON_PROVIDER_SIDE, DELETED_INSTANCE_FQD);
        InstanceMetaData runningInstanceMetaData = createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, RUNNING_INSTANCE_FQDN);
        instanceGroup.setInstanceMetaData(Set.of(deletedInstanceMetaData, runningInstanceMetaData));
        hostGroup.setInstanceGroup(instanceGroup);

        ApiHostRefList apiHostRefList = new ApiHostRefList();
        apiHostRefList.setItems(List.of(createApiHostRef(DELETED_INSTANCE_FQD), createApiHostRef(RUNNING_INSTANCE_FQDN)));
        when(clustersResourceApi.listHosts(STACK_NAME, null, null)).thenReturn(apiHostRefList);
        when(clouderaManagerApiFactory.getClustersResourceApi(client)).thenReturn(clustersResourceApi);

        Set<String> hostNames = Set.of(DELETED_INSTANCE_FQD, RUNNING_INSTANCE_FQDN);

        Map<String, InstanceMetaData> result = underTest.collectHostsToRemove(getStack(), hostGroup, hostNames, client);

        assertThat(result)
                .containsValue(runningInstanceMetaData)
                .containsValue(deletedInstanceMetaData);
    }

    private InstanceMetaData createInstanceMetadata(InstanceStatus servicesHealthy, String runningInstanceFqdn) {
        InstanceMetaData runningInstanceMetaData = new InstanceMetaData();
        runningInstanceMetaData.setInstanceStatus(servicesHealthy);
        runningInstanceMetaData.setDiscoveryFQDN(runningInstanceFqdn);
        return runningInstanceMetaData;
    }

    private ApiHostRef createApiHostRef(String deletedInstanceFqd) {
        ApiHostRef deletedInstanceHostRef = new ApiHostRef();
        deletedInstanceHostRef.setHostname(deletedInstanceFqd);
        return deletedInstanceHostRef;
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setName(STACK_NAME);
        return stack;
    }
}
