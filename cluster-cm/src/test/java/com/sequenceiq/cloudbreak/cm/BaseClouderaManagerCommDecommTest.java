package com.sequenceiq.cloudbreak.cm;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public abstract class BaseClouderaManagerCommDecommTest {

    protected ApiHost createGoodHealthApiHostRef(String instanceFqd) {
        return createApiHostRef(instanceFqd, ApiHealthSummary.GOOD);
    }

    protected ApiHost createApiHostRef(String instanceFqd, ApiHealthSummary healthSummary) {
        ApiHost instanceHostRef = new ApiHost();
        instanceHostRef.setHostname(instanceFqd);
        instanceHostRef.setHostId(instanceFqd);
        instanceHostRef.setHealthSummary(healthSummary);
        return instanceHostRef;
    }

    protected ApiHostList createGoodHealthApiHostList(Set<String> hostnames) {
        List<ApiHost> apiHosts = hostnames.stream().map(h -> createGoodHealthApiHostRef(h)).collect(Collectors.toList());
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.setItems(apiHosts);
        return apiHostList;
    }

    protected void mockListClusterHosts(ApiHostList apiHostList, HostsResourceApi hostsResourceApi,
            ClouderaManagerApiFactory clouderaManagerApiFactory, ApiClient client) throws ApiException {
        when(hostsResourceApi.readHosts(null, null, "SUMMARY")).thenReturn(apiHostList);
        when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
    }

    protected InstanceMetaData createRunningInstanceMetadata(String fqdn, String groupName) {
        return createInstanceMetadata(InstanceStatus.SERVICES_HEALTHY, fqdn, groupName);
    }

    protected ApiCommand getApiCommand(BigDecimal commandId) {
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setId(commandId);
        return apiCommand;
    }

    private InstanceMetaData createInstanceMetadata(InstanceStatus servicesHealthy, String runningInstanceFqdn, String instanceGroupName) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(servicesHealthy);
        instanceMetaData.setDiscoveryFQDN(runningInstanceFqdn);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(instanceGroupName);
        instanceMetaData.setInstanceGroup(instanceGroup);
        return instanceMetaData;
    }
}
