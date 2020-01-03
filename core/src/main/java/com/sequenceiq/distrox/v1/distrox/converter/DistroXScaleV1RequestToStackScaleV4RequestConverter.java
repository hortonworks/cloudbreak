package com.sequenceiq.distrox.v1.distrox.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import org.springframework.stereotype.Component;

@Component
public class DistroXScaleV1RequestToStackScaleV4RequestConverter
        extends AbstractConversionServiceAwareConverter<DistroXScaleV1Request, StackScaleV4Request> {

    @Override
    public StackScaleV4Request convert(DistroXScaleV1Request source) {
        StackScaleV4Request stackScaleV4Request = new StackScaleV4Request();
        stackScaleV4Request.setDesiredCount(source.getDesiredCount());
        stackScaleV4Request.setGroup(source.getGroup());
        return stackScaleV4Request;
    }

}
