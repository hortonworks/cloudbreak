package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component("ProvisioningFinishedAction")
public class ProvisioningFinishedAction extends AbstractStackCreationAction<LaunchStackResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningFinishedAction.class);

    @Inject
    private StackCreationService stackCreationService;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    public ProvisioningFinishedAction() {
        super(LaunchStackResult.class);
    }

    @Override
    protected void doExecute(StackContext context, LaunchStackResult payload, Map<Object, Object> variables) {
        Stack stack = stackCreationService.provisioningFinished(context, payload, getStartDateIfExist(variables));
        StackContext newContext = new StackContext(context.getFlowId(), stack, context.getCloudContext(),
                context.getCloudCredential(), context.getCloudStack());
        sendEvent(newContext);
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(context.getStack());
        List<CloudResource> cloudResources = cloudResourceConverter.convert(context.getStack().getResources());
        return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances);
    }

    private Date getStartDateIfExist(Map<Object, Object> variables) {
        Date result = null;
        Object startDateObj = variables.get(StackProvisionConstants.START_DATE);
        if (startDateObj != null && startDateObj instanceof Date) {
            result = (Date) startDateObj;
        }
        return result;
    }

    @Override
    protected Long getStackId(LaunchStackResult payload) {
        return payload.getRequest().getCloudContext().getId();
    }
}
