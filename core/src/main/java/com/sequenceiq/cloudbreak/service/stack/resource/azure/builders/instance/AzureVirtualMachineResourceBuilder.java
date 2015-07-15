package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.instance;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_GCP_AND_AZURE_USER_NAME;
import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.isGateway;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.PORTS;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.RESERVEDIPNAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.VIRTUAL_NETWORK_IP_ADDRESS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.connector.CloudConnectorException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.flow.AzureInstanceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AzureInstances;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureCreateResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureDeleteResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureStartStopContextObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
@Order(3)
public class AzureVirtualMachineResourceBuilder extends AzureSimpleInstanceResourceBuilder {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private PollingService<AzureInstances> azurePollingService;

    @Inject
    private AzureStackUtil azureStackUtil;

    @Inject
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;

    @Inject
    private AzureDeleteResourceStatusCheckerTask azureDeleteResourceStatusCheckerTask;

    @Inject
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Inject
    private AzureInstanceStatusCheckerTask azureInstanceStatusCheckerTask;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, final String region) throws Exception {
        AzureVirtualMachineCreateRequest aCSCR = (AzureVirtualMachineCreateRequest) createResourceRequest;
        try {
            Map<String, Object> props = aCSCR.getProps();
            AzureClient azureClient = aCSCR.getAzureClient();
            HttpResponseDecorator virtualMachineResponse = (HttpResponseDecorator) azureClient.createVirtualMachine(props);
            AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(
                    azureClient, ResourceType.AZURE_VIRTUAL_MACHINE, String.valueOf(props.get(NAME)), aCSCR.getStack(), virtualMachineResponse);
            azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
        } catch (Exception ex) {
            throw checkException(ex);
        }
        return true;
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String cloudServiceName = filterResourcesByType(resources, ResourceType.AZURE_CLOUD_SERVICE).get(0).getResourceName();
        String virtualMachineName = getResourceNameService().resourceName(resourceType(), cloudServiceName);
        return Arrays.asList(new Resource(resourceType(), virtualMachineName, stack, instanceGroup.orNull().getGroupName()));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        try {
            Stack stack = stackRepository.findById(provisionContextObject.getStackId());
            AzureTemplate azureTemplate = (AzureTemplate) instanceGroup.orNull().getTemplate();
            AzureCredential azureCredential = (AzureCredential) stack.getCredential();
            String resourceName = buildResources.get(0).getResourceName();
            byte[] encoded = Base64.encodeBase64(resourceName.getBytes());
            String label = new String(encoded);
            CloudRegion azureLocation = CloudRegion.valueOf(stack.getRegion());
            int storageIndex = provisionContextObject.isUseGlobalStorageAccount() ? AzureStackUtil.GLOBAL_STORAGE
                    : provisionContextObject.setAndGetStorageAccountIndex(azureTemplate.getVolumeCount() + AzureStackUtil.ROOTFS_COUNT);
            LOGGER.info("Storage index selected: {} for {}", storageIndex, resourceName);
            String osStorageName = azureStackUtil.getOSStorageName(stack, azureLocation, storageIndex);
            Stack stackWithSecurityGroup = stackRepository.findByIdWithSecurityGroup(stack.getId());
            Map<String, Object> props = new HashMap<>();
            props.put(NAME, resourceName);
            props.put(DEPLOYMENTSLOT, PRODUCTION);
            props.put(LABEL, label);
            props.put(IMAGENAME, azureStackUtil.getOsImageName(stack, azureLocation, storageIndex));
            props.put(IMAGESTOREURI, buildImageStoreUri(osStorageName, resourceName));
            props.put(HOSTNAME, resourceName);
            props.put(USERNAME, CB_GCP_AND_AZURE_USER_NAME);
            props.put(SSHKEYS, prepareKeys(provisionContextObject, buildResources, azureCredential));
            props.put(STORAGE_NAME, osStorageName);
            props.put(AFFINITYGROUP, provisionContextObject.getAffinityGroupName());
            if (azureTemplate.getVolumeCount() > 0) {
                props.put(DISKS, generateDisksProperty(azureTemplate));
            }
            props.put(SERVICENAME, resourceName);
            props.put(SUBNETNAME, provisionContextObject.filterResourcesByType(ResourceType.AZURE_NETWORK).get(0).getResourceName());
            props.put(VIRTUAL_NETWORK_IP_ADDRESS, findNextValidIp(provisionContextObject));
            props.put(CUSTOMDATA, new String(Base64.encodeBase64(userData.orNull().getBytes())));
            props.put(VIRTUALNETWORKNAME, provisionContextObject.filterResourcesByType(ResourceType.AZURE_NETWORK).get(0).getResourceName());
            props.put(PORTS, NetworkUtils.getPorts(Optional.fromNullable(stackWithSecurityGroup)));
            props.put(VMTYPE, azureTemplate.getVmType().vmType().replaceAll(" ", ""));
            if (isGateway(instanceGroup.orNull().getInstanceGroupType())) {
                props.put(RESERVEDIPNAME, stack.getResourceByType(ResourceType.AZURE_RESERVED_IP).getResourceName());
            }
            return new AzureVirtualMachineCreateRequest(props, resources, buildResources, stack, instanceGroup.orNull());
        } catch (Exception e) {
            throw new CloudConnectorException(e);
        }
    }

    private List<SshKey> prepareKeys(AzureProvisionContextObject provisionObject, List<Resource> resources, AzureCredential credential) throws Exception {
        List<SshKey> result = new ArrayList<>();
        result.add(new SshKey(azureStackUtil.createX509Certificate(credential).getSha1Fingerprint().toUpperCase(),
                String.format("/home/%s/.ssh/authorized_keys", CB_GCP_AND_AZURE_USER_NAME)));
        return result;
    }

    private List<Integer> generateDisksProperty(AzureTemplate azureTemplate) {
        List<Integer> disks = new ArrayList<>();
        for (int i = 0; i < azureTemplate.getVolumeCount(); i++) {
            disks.add(azureTemplate.getVolumeSize());
        }
        return disks;
    }

    private String findNextValidIp(AzureProvisionContextObject provisionContextObject) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String subnetCIDR = stack.getNetwork().getSubnetCIDR();
        String ip = azureStackUtil.getFirstAssignableIPOfSubnet(subnetCIDR);
        String lastAssignableIP = azureStackUtil.getLastAssignableIPOfSubnet(subnetCIDR);

        boolean found = false;
        while (!found && !ip.equals(lastAssignableIP)) {
            ip = azureStackUtil.getNextIPAddress(ip);
            found = provisionContextObject.putIfAbsent(ip);
        }
        return ip;
    }

    private String buildImageStoreUri(String storageName, String vmName) {
        return String.format("http://%s.blob.core.windows.net/vhd-store/%s.vhd", storageName, vmName);
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject aDCO, String region) throws Exception {
        Stack stack = stackRepository.findByIdLazy(aDCO.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        try {
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, resource.getResourceName());
            props.put(NAME, resource.getResourceName());
            AzureClient azureClient = azureStackUtil.createAzureClient(credential);
            HttpResponseDecorator deleteVirtualMachineResult = (HttpResponseDecorator) azureClient.deleteVirtualMachine(props);
            AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(
                    azureClient, ResourceType.AZURE_VIRTUAL_MACHINE, props.get(NAME), stack, deleteVirtualMachineResult);
            azureResourcePollerObjectPollingService.pollWithTimeout(azureDeleteResourceStatusCheckerTask, azureResourcePollerObject,
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
        } catch (HttpResponseException ex) {
            httpResponseExceptionHandler(ex, resource.getResourceName(), stack.getOwner());
        } catch (Exception ex) {
            throw new AzureResourceException(ex);
        }
        return true;
    }

    @Override
    public void start(AzureStartStopContextObject aSSCO, Resource resource, String region) {
        Stack stack = stackRepository.findById(aSSCO.getStack().getId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        setStackState(aSSCO.getStack().getId(), resource, azureStackUtil.createAzureClient(credential), false);
        azurePollingService.pollWithTimeout(
                azureInstanceStatusCheckerTask,
                new AzureInstances(aSSCO.getStack(), azureStackUtil.createAzureClient(credential), Arrays.asList(resource.getResourceName()), "Running"),
                POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_AMBARI_OPS,
                MAX_FAILURE_COUNT);

    }

    @Override
    public void stop(AzureStartStopContextObject aSSCO, Resource resource, String region) {
        Stack stack = stackRepository.findById(aSSCO.getStack().getId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        setStackState(aSSCO.getStack().getId(), resource, azureStackUtil.createAzureClient(credential), true);
        azurePollingService.pollWithTimeout(
                new AzureInstanceStatusCheckerTask(),
                new AzureInstances(aSSCO.getStack(), azureStackUtil.createAzureClient(credential), Arrays.asList(resource.getResourceName()), "Suspended"),
                POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_AMBARI_OPS);

    }

    private void setStackState(Long stackId, Resource resource, AzureClient azureClient, boolean stopped) {
        try {
            Map<String, String> vmContext = createVMContext(resource.getResourceName());
            if (stopped) {
                if ("Running".equals(azureClient.getVirtualMachineState(vmContext))) {
                    azureClient.stopVirtualMachine(vmContext);
                } else {
                    LOGGER.info("Instance is not in Running state - won't stop it.");
                }
            } else {
                if ("Suspended".equals(azureClient.getVirtualMachineState(vmContext))) {
                    azureClient.startVirtualMachine(vmContext);
                } else {
                    LOGGER.info("Instance is not in Suspended state - won't start it.");
                }
            }
        } catch (Exception e) {
            throw new AzureResourceException(String.format("Failed to %s AZURE instances on stack: %s", stopped ? "stop" : "start", stackId), e);
        }
    }

    private Map<String, String> createVMContext(String vmName) {
        Map<String, String> context = new HashMap<>();
        context.put(SERVICENAME, vmName);
        context.put(NAME, vmName);
        return context;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_VIRTUAL_MACHINE;
    }

    public class AzureVirtualMachineCreateRequest extends CreateResourceRequest {
        private Map<String, Object> props = new HashMap<>();
        private List<Resource> resources;
        private Stack stack;
        private InstanceGroup instanceGroup;

        public AzureVirtualMachineCreateRequest(Map<String, Object> props, List<Resource> resources, List<Resource> buildNames,
                Stack stack, InstanceGroup instanceGroup) {
            super(buildNames);
            this.stack = stack;
            this.props = props;
            this.resources = resources;
            this.instanceGroup = instanceGroup;
        }

        public Map<String, Object> getProps() {
            return props;
        }

        public AzureClient getAzureClient() {
            return azureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        }

        public Stack getStack() {
            return stack;
        }

        public List<Resource> getResources() {
            return resources;
        }

        public InstanceGroup getInstanceGroup() {
            return instanceGroup;
        }
    }

    private class SshKey {
        private String fingerPrint;
        private String publicKeyPath;

        public SshKey(String fingerPrint, String publicKeyPath) {
            this.fingerPrint = fingerPrint;
            this.publicKeyPath = publicKeyPath;
        }

        public String getFingerPrint() {
            return fingerPrint;
        }

        public String getPublicKeyPath() {
            return publicKeyPath;
        }
    }


}
