package com.sequenceiq.environment.tags.v1.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.api.v1.tags.endpoint.AccountTagEndpoint;
import com.sequenceiq.environment.api.v1.tags.model.request.AccountTagRequests;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.service.AccountTagService;
import com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService;
import com.sequenceiq.environment.tags.v1.converter.AccountTagToAccountTagRequestsConverter;
import com.sequenceiq.environment.tags.v1.converter.AccountTagToAccountTagResponsesConverter;
import com.sequenceiq.environment.tags.v1.converter.AccountTagsRequestToAccountTagConverter;
import com.sequenceiq.notification.NotificationController;

@Controller
@Transactional(TxType.NEVER)
public class AccountTagController extends NotificationController implements AccountTagEndpoint {

    private final AccountTagService accountTagService;

    private final AccountTagToAccountTagRequestsConverter accountTagToAccountTagRequestsConverter;

    private final AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter;

    private final AccountTagsRequestToAccountTagConverter accountTagsRequestToAccountTagConverter;

    private final DefaultInternalAccountTagService defaultInternalAccountTagService;

    public AccountTagController(AccountTagService accountTagService,
                                AccountTagToAccountTagRequestsConverter accountTagToAccountTagRequestsConverter,
                                AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter,
                                AccountTagsRequestToAccountTagConverter accountTagsRequestToAccountTagConverter,
                                DefaultInternalAccountTagService defaultInternalAccountTagService) {
        this.accountTagService = accountTagService;
        this.accountTagToAccountTagRequestsConverter = accountTagToAccountTagRequestsConverter;
        this.accountTagToAccountTagResponsesConverter = accountTagToAccountTagResponsesConverter;
        this.accountTagsRequestToAccountTagConverter = accountTagsRequestToAccountTagConverter;
        this.defaultInternalAccountTagService = defaultInternalAccountTagService;
    }

    @Override
    public AccountTagResponses list() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Set<AccountTag> accountTags = accountTagService.get(accountId);
        List<AccountTagResponse> accountTagResponses = accountTagToAccountTagResponsesConverter.convert(accountTags);
        accountTagResponses.addAll(defaultInternalAccountTagService.getDefaults().getResponses());
        return new AccountTagResponses(new HashSet<>(accountTagResponses));
    }

    @Override
    public AccountTagResponses put(@Valid AccountTagRequests request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        List<AccountTag> accountTags = accountTagsRequestToAccountTagConverter.convert(request.getTags());
        accountTags = accountTagService.create(accountTags, accountId);
        return new AccountTagResponses(new HashSet<>(accountTagToAccountTagResponsesConverter.convert(accountTags)));
    }
}
