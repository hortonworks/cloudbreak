package com.sequenceiq.environment.tags.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.tags.model.request.AccountTagRequest;
import com.sequenceiq.environment.tags.domain.AccountTag;

@Component
public class AccountTagToAccountTagRequestsConverter {

    public AccountTagRequest convert(AccountTag source) {
        AccountTagRequest request = new AccountTagRequest();
        request.setKey(source.getTagKey());
        request.setValue(source.getTagValue());
        return request;
    }

}
