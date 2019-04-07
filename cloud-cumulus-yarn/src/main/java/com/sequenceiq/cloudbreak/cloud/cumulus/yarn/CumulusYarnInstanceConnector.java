package com.sequenceiq.cloudbreak.cloud.cumulus.yarn;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cb.yarn.service.api.ApiException;
import org.apache.cb.yarn.service.api.impl.DefaultApi;
import org.apache.cb.yarn.service.api.records.Container;
import org.apache.cb.yarn.service.api.records.ContainerState;
import org.apache.cb.yarn.service.api.records.Service;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.client.CumulusYarnClient;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.util.CumulusYarnContainerStatus;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.util.CumulusYarnResourceNameHelper;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

@org.springframework.stereotype.Service
public class CumulusYarnInstanceConnector implements InstanceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CumulusYarnInstanceConnector.class);

    @Inject
    private CumulusYarnClient client;

    @Inject
    private CumulusYarnResourceNameHelper cumulusYarnResourceNameHelper;

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Start instances operation is not supported on YARN");
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Stop instances operation is not supported on YARN");
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {
        DefaultApi api = client.createApi(authenticatedContext);
        try {
            Service service = api.appV1ServicesServiceNameGet(cumulusYarnResourceNameHelper.createApplicationName(authenticatedContext));
            Map<String, ContainerState> containerStateById = service.getComponents().stream()
                    .flatMap(component -> component.getContainers().stream())
                    .filter(container -> StringUtils.isNotBlank(container.getId()))
                    .collect(Collectors.toMap(Container::getId, Container::getState));
            return vms.stream()
                    .filter(cloudInstance -> StringUtils.isNotBlank(cloudInstance.getInstanceId()))
                    .map(cloudInstance -> new CloudVmInstanceStatus(cloudInstance,
                            CumulusYarnContainerStatus.mapInstanceStatus(containerStateById.get(cloudInstance.getInstanceId()))))
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            throw new CloudConnectorException("Couldn't get service state from Cumulus Yarn", e);
        }
    }

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        throw new CloudOperationNotSupportedException("Host key verification is disabled on YARN");
    }
}
