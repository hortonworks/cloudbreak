package com.sequenceiq.distrox.v1.distrox.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;

@Component
public class DistroXScaleV1RequestToStackScaleV4RequestConverter
        extends AbstractConversionServiceAwareConverter<DistroXScaleV1Request, StackScaleV4Request> {

    @Inject
    private StackService stackService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public StackScaleV4Request convert(DistroXScaleV1Request source) {
        StackScaleV4Request stackScaleV4Request = new StackScaleV4Request();
        stackScaleV4Request.setDesiredCount(source.getDesiredCount());
        stackScaleV4Request.setGroup(source.getGroup());
        return stackScaleV4Request;
    }
}
