package com.sequenceiq.environment.tags.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.tags.model.AccountTagStatus;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.tags.domain.AccountTag;

@Component
public class AccountTagToAccountTagResponsesConverter {

    public AccountTagResponse convert(AccountTag source) {
        AccountTagResponse response = new AccountTagResponse();
        response.setKey(source.getTagKey());
        response.setValue(source.getTagValue());
        response.setAccountId(source.getAccountId());
        response.setResourceCrn(source.getResourceCrn());
        response.setStatus(AccountTagStatus.USER_MANAGED);
        return response;
    }
}
