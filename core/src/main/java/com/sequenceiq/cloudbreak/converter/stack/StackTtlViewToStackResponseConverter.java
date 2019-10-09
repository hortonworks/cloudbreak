package com.sequenceiq.cloudbreak.converter.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;

@Component
public class StackTtlViewToStackResponseConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTtlViewToStackResponseConverter.class);

    public StackResponse convert(StackTtlView source) {
        StackResponse stackJson = new StackResponse();

        stackJson.setId(source.getId());
        stackJson.setName(source.getName());
        stackJson.setOwner(source.getOwner());
        stackJson.setAccount(source.getAccount());

        return stackJson;
    }
}
