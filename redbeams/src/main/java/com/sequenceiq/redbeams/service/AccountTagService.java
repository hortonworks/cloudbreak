package com.sequenceiq.redbeams.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.api.tag.model.Tags;
import com.sequenceiq.environment.api.v1.tags.endpoint.AccountTagEndpoint;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;

@Service
public class AccountTagService {

    @Inject
    private AccountTagEndpoint accountTagEndpoint;

    public Tags list() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        AccountTagResponses list = ThreadBasedUserCrnProvider.doAsInternalActor(() -> accountTagEndpoint.listInAccount(accountId));
        Tags tags = new Tags();
        list.getResponses().forEach(tag -> tags.addTag(tag.getKey(), tag.getValue()));
        return tags;
    }
}
