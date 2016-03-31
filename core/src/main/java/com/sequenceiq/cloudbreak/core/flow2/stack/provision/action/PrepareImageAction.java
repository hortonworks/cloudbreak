package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@Component("PrepareImageAction")
public class PrepareImageAction extends AbstractStackCreationAction<SetupResult> {
    @Inject
    private ImageService imageService;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    public PrepareImageAction() {
        super(SetupResult.class);
    }

    @Override
    protected void doExecute(StackContext context, SetupResult payload, Map<Object, Object> variables) {
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        CloudStack cloudStack = cloudStackConverter.convert(context.getStack());
        Image image = imageService.getImage(context.getCloudContext().getId());
        return new PrepareImageRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudStack, image);
    }

    @Override
    protected Long getStackId(SetupResult payload) {
        return payload.getRequest().getCloudContext().getId();
    }
}
