package com.sequenceiq.environment.tags.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.tags.model.request.AccountTagRequest;
import com.sequenceiq.environment.tags.domain.AccountTag;

@Component
public class AccountTagsRequestToAccountTagConverter {

    public AccountTag convert(AccountTagRequest source) {
        AccountTag accountTag = new AccountTag();
        accountTag.setTagKey(source.getKey());
        accountTag.setTagValue(source.getValue());
        return accountTag;
    }
}
