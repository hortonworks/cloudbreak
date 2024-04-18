package com.sequenceiq.cloudbreak.cloud.yarn;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

@Service
public class YarnInstanceConnector implements InstanceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(YarnInstanceConnector.class);

    @Inject
    private YarnApplicationDetailsService yarnApplicationDetailsService;

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Start instances operation is not supported on YARN");
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Stop instances operation is not supported on YARN");
    }

    @Override
    public List<CloudVmInstanceStatus> reboot(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Reboot instances operation is not supported on YARN");
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {
        Map<String, List<CloudInstance>> vmsByApplicationName = vms.stream()
                .filter(vm -> vm.hasParameter(CloudInstance.APPLICATION_NAME))
                .collect(Collectors.groupingBy(vm -> vm.getStringParameter(CloudInstance.APPLICATION_NAME)));
        if (vmsByApplicationName.isEmpty()) {
            throw new CloudOperationNotSupportedException("Instances' states check operation is not supported on YARN without application name");
        }
        return vmsByApplicationName.keySet().stream()
                .map(applicationName -> checkApplication(authenticatedContext, applicationName, vmsByApplicationName.get(applicationName)))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<CloudVmInstanceStatus> checkApplication(AuthenticatedContext authenticatedContext, String applicationName, List<CloudInstance> vms) {
        return yarnApplicationDetailsService.collect(authenticatedContext, applicationName, vms).stream()
                .map(CloudVmMetaDataStatus::getCloudVmInstanceStatus)
                .toList();
    }

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        throw new CloudOperationNotSupportedException("Host key verification is disabled on YARN");
    }
}
