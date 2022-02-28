package com.sequenceiq.distrox.v1.distrox.converter.cli;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.v1.distrox.converter.cli.distrox.DistroXV1RequestToCliRequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.cli.stack.StackRequestToCliRequestConverter;

@Component
public class DelegatingRequestToCliRequestConverter {

    @Inject
    private List<StackRequestToCliRequestConverter> stackConverters;

    @Inject
    private List<DistroXV1RequestToCliRequestConverter> distroxConverters;

    public Object convertStack(StackV4Request source) {
        return stackConverters.stream()
                .filter(c -> c.supportedPlatform().equals(source.getCloudPlatform()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No converter found for cloud platform: " + source.getCloudPlatform()))
                .convert(source);
    }

    public Object convertDistroX(DistroXV1Request source) {
        return distroxConverters.stream()
                .filter(c -> c.supportedPlatform().equals(source.getCloudPlatform()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No converter found for cloud platform: " + source.getCloudPlatform()))
                .convert(source);
    }
}
