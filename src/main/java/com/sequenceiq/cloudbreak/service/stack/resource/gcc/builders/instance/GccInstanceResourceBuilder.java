package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccInstanceCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccInstanceReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveCheckerStatus;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccRemoveReadyPollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.GccSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;

@Component
@Order(3)
public class GccInstanceResourceBuilder extends GccSimpleInstanceResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private GccInstanceCheckerStatus gccInstanceReadyCheckerStatus;
    @Autowired
    private PollingService<GccInstanceReadyPollerObject> gccInstanceReadyPollerObjectPollingService;
    @Autowired
    private GccRemoveCheckerStatus gccRemoveCheckerStatus;
    @Autowired
    private PollingService<GccRemoveReadyPollerObject> gccRemoveReadyPollerObjectPollingService;
    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public List<Resource> create(GccProvisionContextObject po) throws Exception {
        return create(po, 0);
    }

    @Override
    public List<Resource> create(GccProvisionContextObject po, int index) throws Exception {
        Stack stack = stackRepository.findById(po.getStackId());
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential credential = (GccCredential) stack.getCredential();
        String name = String.format("%s-%s", stack.getName(), index);
        Instance instance = new Instance();
        instance.setMachineType(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                po.getProjectId(), gccTemplate.getGccZone().getValue(), gccTemplate.getGccInstanceType().getValue()));
        instance.setName(name);
        instance.setCanIpForward(Boolean.TRUE);
        instance.setNetworkInterfaces(po.getNetworkInterfaces());
        instance.setDisks(po.getDiskMap().get(name));
        Metadata metadata = new Metadata();
        metadata.setItems(Lists.<Metadata.Items>newArrayList());

        Metadata.Items item1 = new Metadata.Items();
        item1.setKey("sshKeys");
        item1.setValue("ubuntu:" + credential.getPublicKey());

        Metadata.Items item2 = new Metadata.Items();
        item2.setKey("startup-script");
        item2.setValue(po.getUserData());

        metadata.getItems().add(item1);
        metadata.getItems().add(item2);
        instance.setMetadata(metadata);
        Compute.Instances.Insert ins =
                po.getCompute().instances().insert(credential.getProjectId(), gccTemplate.getGccZone().getValue(), instance);

        ins.setPrettyPrint(Boolean.TRUE);
        ins.execute();
        GccInstanceReadyPollerObject gccInstanceReady = new GccInstanceReadyPollerObject(po.getCompute(), stack, name);
        gccInstanceReadyPollerObjectPollingService.pollWithTimeout(gccInstanceReadyCheckerStatus, gccInstanceReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        return Arrays.asList(new Resource(resourceType(), name, stack));
    }

    @Override
    public Boolean delete(Resource resource, GccDeleteContextObject d) throws Exception {
        try {
            Stack stack = stackRepository.findById(d.getStackId());
            GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
            GccCredential gccCredential = (GccCredential) stack.getCredential();
            Operation execute = d.getCompute().instances()
                    .delete(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), resource.getResourceName()).execute();
            Compute.ZoneOperations.Get zoneOperations = createZoneOperations(d.getCompute(), gccCredential, gccTemplate, execute);
            Compute.GlobalOperations.Get globalOperations = createGlobalOperations(d.getCompute(), gccCredential, gccTemplate, execute);
            GccRemoveReadyPollerObject gccRemoveReady =
                    new GccRemoveReadyPollerObject(zoneOperations, globalOperations, stack.getId(), resource.getResourceName());
            gccRemoveReadyPollerObjectPollingService.pollWithTimeout(gccRemoveCheckerStatus, gccRemoveReady, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        } catch (GoogleJsonResponseException ex) {
            exceptionHandler(ex, resource.getResourceName());
        } catch (IOException e) {
            throw new InternalServerException(e.getMessage());
        }
        return true;
    }

    @Override
    public Optional<String> describe(Resource resource, GccDescribeContextObject dco) throws Exception {
        try {
            Stack stack = stackRepository.findById(dco.getStackId());
            GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
            GccCredential gccCredential = (GccCredential) stack.getCredential();
            Compute.Instances.Get getVm = dco.getCompute().instances().get(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(),
                    resource.getResourceName());
            return Optional.fromNullable(getVm.execute().toPrettyString());
        } catch (IOException e) {
            return Optional.fromNullable(jsonHelper.createJsonFromString(String.format("{\"VirtualMachine\": {%s}}", ERROR)).toString());
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_INSTANCE;
    }

}
