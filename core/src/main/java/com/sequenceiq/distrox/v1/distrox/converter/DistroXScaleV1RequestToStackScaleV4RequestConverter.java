package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.stack.StackService;

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
        Optional<Stack> stack = stackService.findStackByNameAndWorkspaceId(source.getName(), workspaceService.getForCurrentUser().getId());
        if (stack.isPresent()) {
            stackScaleV4Request.setStackId(stack.get().getId());
        } else {
            throw new BadRequestException("DistroX cluster does not exist with name: " + source.getName());
        }
        return stackScaleV4Request;
    }
}
