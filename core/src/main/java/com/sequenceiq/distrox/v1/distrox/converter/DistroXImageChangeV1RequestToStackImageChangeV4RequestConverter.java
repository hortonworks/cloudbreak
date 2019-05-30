package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.distrox.api.v1.distrox.model.DistroXImageChangeV1Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class DistroXImageChangeV1RequestToStackImageChangeV4RequestConverter
        extends AbstractConversionServiceAwareConverter<DistroXImageChangeV1Request, StackImageChangeV4Request> {
    @Override
    public StackImageChangeV4Request convert(DistroXImageChangeV1Request source) {
        StackImageChangeV4Request stackImageChangeV4Request = new StackImageChangeV4Request();
        stackImageChangeV4Request.setImageId(source.getImageId());
        stackImageChangeV4Request.setImageCatalogName(source.getImageCatalogName());
        return stackImageChangeV4Request;
    }
}
