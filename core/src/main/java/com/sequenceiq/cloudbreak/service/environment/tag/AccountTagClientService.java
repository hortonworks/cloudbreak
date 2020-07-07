package com.sequenceiq.cloudbreak.service.environment.tag;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.tag.model.Tags;
import com.sequenceiq.environment.api.v1.tags.endpoint.AccountTagEndpoint;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;

@Service
public class AccountTagClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountTagClientService.class);

    @Inject
    private AccountTagEndpoint accountTagEndpoint;

    public Tags list() {
        try {
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            AccountTagResponses list = ThreadBasedUserCrnProvider.doAsInternalActor(() -> accountTagEndpoint.listInAccount(accountId));
            Tags tags = new Tags();
            list.getResponses().forEach(tag -> tags.addTag(tag.getKey(), tag.getValue()));
            return tags;
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET AccountTags with account id, due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
