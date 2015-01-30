package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.StackCreationFailureException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.AzureVmType;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.X509Certificate;
import com.sequenceiq.cloudbreak.service.stack.flow.AzureInstanceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AzureInstances;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureStartStopContextObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
@Order(3)
public class AzureVirtualMachineResourceBuilder extends AzureSimpleInstanceResourceBuilder {

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private PollingService<AzureInstances> azurePollingService;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Autowired
    private AzureResourceStatusCheckerTask azureResourceStatusCheckerTask;

    @Autowired
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, final String region) throws Exception {
        AzureVirtualMachineCreateRequest aCSCR = (AzureVirtualMachineCreateRequest) createResourceRequest;
        HttpResponseDecorator virtualMachineResponse = (HttpResponseDecorator) aCSCR.getAzureClient().createVirtualMachine(aCSCR.getProps());
        AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(aCSCR.getAzureClient(), virtualMachineResponse, aCSCR.getStack());
        azureResourcePollerObjectPollingService.pollWithTimeout(azureResourceStatusCheckerTask, azureResourcePollerObject,
                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        return true;
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String vmName = filterResourcesByType(resources, ResourceType.AZURE_CLOUD_SERVICE).get(0).getResourceName();
        return Arrays.asList(new Resource(resourceType(), vmName, stack, instanceGroup.orNull().getGroupName()));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String internalIp = "172.16.0." + (index + VALID_IP_RANGE_START);
        AzureTemplate azureTemplate = (AzureTemplate) instanceGroup.orNull().getTemplate();
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();
        byte[] encoded = Base64.encodeBase64(buildResources.get(0).getResourceName().getBytes());
        String label = new String(encoded);
        Map<String, Object> props = new HashMap<>();
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("Ambari", "8080", "8080", "tcp"));
        ports.add(new Port("Consul", "8500", "8500", "tcp"));
        ports.add(new Port("NameNode", "50070", "50070", "tcp"));
        ports.add(new Port("RM Web", "8088", "8088", "tcp"));
        ports.add(new Port("RM Scheduler", "8030", "8030", "tcp"));
        ports.add(new Port("RM IPC", "8050", "8050", "tcp"));
        ports.add(new Port("Job History Server", "19888", "19888", "tcp"));
        ports.add(new Port("HBase Master", "60010", "60010", "tcp"));
        ports.add(new Port("Falcon", "15000", "15000", "tcp"));
        ports.add(new Port("Storm", "8744", "8744", "tcp"));
        ports.add(new Port("Oozie", "11000", "11000", "tcp"));
        ports.add(new Port("HTTP", "80", "80", "tcp"));
        props.put(NAME, buildResources.get(0).getResourceName());
        props.put(DEPLOYMENTSLOT, PRODUCTION);
        props.put(LABEL, label);
        props.put(IMAGENAME, azureStackUtil.getOsImageName(stack.getCredential(), AzureLocation.valueOf(stack.getRegion()), stack.getImage()));
        props.put(IMAGESTOREURI, buildimageStoreUri(provisionContextObject.getCommonName(), buildResources.get(0).getResourceName()));
        props.put(HOSTNAME, buildResources.get(0).getResourceName());
        props.put(USERNAME, DEFAULT_USER_NAME);
        X509Certificate sshCert = null;
        try {
            sshCert = azureStackUtil.createX509Certificate(azureCredential);
        } catch (FileNotFoundException e) {
            throw new StackCreationFailureException(e);
        } catch (CertificateException e) {
            throw new StackCreationFailureException(e);
        }
        try {
            props.put(SSHPUBLICKEYFINGERPRINT, sshCert.getSha1Fingerprint().toUpperCase());
        } catch (CertificateEncodingException e) {
            throw new StackCreationFailureException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new StackCreationFailureException(e);
        }
        props.put(SSHPUBLICKEYPATH, String.format("/home/%s/.ssh/authorized_keys", DEFAULT_USER_NAME));
        props.put(AFFINITYGROUP, provisionContextObject.getCommonName());
        if (azureTemplate.getVolumeCount() > 0) {
            List<Integer> disks = new ArrayList<>();
            for (int i = 0; i < azureTemplate.getVolumeCount(); i++) {
                disks.add(azureTemplate.getVolumeSize());
            }
            props.put(DISKS, disks);
        }

        props.put(SERVICENAME, buildResources.get(0).getResourceName());
        props.put(SUBNETNAME, provisionContextObject.filterResourcesByType(ResourceType.AZURE_NETWORK).get(0).getResourceName());
        props.put(VIRTUAL_NETWORK_IP_ADDRESS, internalIp);
        props.put(CUSTOMDATA, new String(Base64.encodeBase64(provisionContextObject.getUserData().getBytes())));
        props.put(VIRTUALNETWORKNAME, provisionContextObject.filterResourcesByType(ResourceType.AZURE_NETWORK).get(0).getResourceName());
        props.put(PORTS, ports);
        props.put(VMTYPE, AzureVmType.valueOf(azureTemplate.getVmType()).vmType().replaceAll(" ", ""));
        return new AzureVirtualMachineCreateRequest(props,
                azureStackUtil.createAzureClient(azureCredential), resources, buildResources, stack, instanceGroup.orNull());
    }

    private String buildimageStoreUri(String commonName, String vmName) {
        return String.format("http://%s.blob.core.windows.net/vhd-store/%s.vhd", commonName, vmName);
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject aDCO, String region) throws Exception {
        Stack stack = stackRepository.findById(aDCO.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        try {
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, resource.getResourceName());
            props.put(NAME, resource.getResourceName());
            AzureClient azureClient = azureStackUtil.createAzureClient(credential);
            HttpResponseDecorator deleteVirtualMachineResult = (HttpResponseDecorator) azureClient.deleteVirtualMachine(props);
            AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(aDCO.getAzureClient(), deleteVirtualMachineResult,
                    stack);
            azureResourcePollerObjectPollingService.pollWithTimeout(azureResourceStatusCheckerTask, azureResourcePollerObject,
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (HttpResponseException ex) {
            httpResponseExceptionHandler(ex, resource.getResourceName(), stack.getOwner(), stack);
        } catch (Exception ex) {
            throw new InternalServerException(ex.getMessage());
        }
        return true;
    }

    @Override
    public Boolean start(AzureStartStopContextObject aSSCO, Resource resource, String region) {
        Stack stack = stackRepository.findById(aSSCO.getStack().getId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        boolean started = setStackState(aSSCO.getStack().getId(), resource, azureStackUtil.createAzureClient(credential), false);
        if (started) {
            azurePollingService.pollWithTimeout(
                    new AzureInstanceStatusCheckerTask(),
                    new AzureInstances(aSSCO.getStack(), azureStackUtil.createAzureClient(credential), Arrays.asList(resource.getResourceName()), "Running"),
                    POLLING_INTERVAL,
                    MAX_ATTEMPTS_FOR_AMBARI_OPS);
            return true;
        }
        return false;
    }

    @Override
    public Boolean stop(AzureStartStopContextObject aSSCO, Resource resource, String region) {
        Stack stack = stackRepository.findById(aSSCO.getStack().getId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        return setStackState(aSSCO.getStack().getId(), resource, azureStackUtil.createAzureClient(credential), true);
    }

    private boolean setStackState(Long stackId, Resource resource, AzureClient azureClient, boolean stopped) {
        boolean result = true;
        try {
            Map<String, String> vmContext = createVMContext(resource.getResourceName());
            if (stopped) {
                azureClient.stopVirtualMachine(vmContext);
            } else {
                azureClient.startVirtualMachine(vmContext);
            }

        } catch (Exception e) {
            LOGGER.error(String.format("Failed to %s AZURE instances on stack: %s", stopped ? "stop" : "start", stackId));
            result = false;
        }
        return result;
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
        private AzureClient azureClient;
        private List<Resource> resources;
        private Stack stack;
        private InstanceGroup instanceGroup;

        public AzureVirtualMachineCreateRequest(Map<String, Object> props, AzureClient azureClient, List<Resource> resources, List<Resource> buildNames,
                Stack stack, InstanceGroup instanceGroup) {
            super(buildNames);
            this.stack = stack;
            this.props = props;
            this.azureClient = azureClient;
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

    public class Port {

        private String localPort;
        private String name;
        private String port;
        private String protocol;

        public Port() {
        }

        public Port(String name, String port, String localPort, String protocol) {
            this.name = name;
            this.localPort = localPort;
            this.port = port;
            this.protocol = protocol;
        }
    }

}
