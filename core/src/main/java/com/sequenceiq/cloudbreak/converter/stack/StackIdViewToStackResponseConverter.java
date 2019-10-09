package com.sequenceiq.cloudbreak.converter.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;

@Component
public class StackIdViewToStackResponseConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackIdViewToStackResponseConverter.class);

    public StackV4Response convert(StackIdView source) {
        StackV4Response stackJson = new StackV4Response();

        stackJson.setId(source.getId());
        stackJson.setName(source.getName());
        stackJson.setCrn(source.getCrn());

        return stackJson;
    }
}
