package com.sequenceiq.environment.tags.v1.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.common.api.tag.model.Tags;
import com.sequenceiq.environment.api.v1.tags.endpoint.AccountTagEndpoint;
import com.sequenceiq.environment.api.v1.tags.model.request.AccountTagRequests;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;
import com.sequenceiq.environment.api.v1.tags.model.response.GeneratedAccountTagResponse;
import com.sequenceiq.environment.api.v1.tags.model.response.GeneratedAccountTagResponses;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.service.AccountTagService;
import com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService;
import com.sequenceiq.environment.tags.v1.converter.AccountTagToAccountTagRequestsConverter;
import com.sequenceiq.environment.tags.v1.converter.AccountTagToAccountTagResponsesConverter;
import com.sequenceiq.environment.tags.v1.converter.AccountTagsRequestToAccountTagConverter;
import com.sequenceiq.notification.NotificationController;

@Controller
@AuthorizationResource
@Transactional(TxType.NEVER)
public class AccountTagController extends NotificationController implements AccountTagEndpoint {

    private final AccountTagService accountTagService;

    private final AccountTagToAccountTagRequestsConverter accountTagToAccountTagRequestsConverter;

    private final AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter;

    private final AccountTagsRequestToAccountTagConverter accountTagsRequestToAccountTagConverter;

    private final DefaultInternalAccountTagService defaultInternalAccountTagService;

    private final EnvironmentService environmentService;

    public AccountTagController(AccountTagService accountTagService,
                                AccountTagToAccountTagRequestsConverter accountTagToAccountTagRequestsConverter,
                                AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter,
                                AccountTagsRequestToAccountTagConverter accountTagsRequestToAccountTagConverter,
                                DefaultInternalAccountTagService defaultInternalAccountTagService,
                                EnvironmentService environmentService) {
        this.accountTagService = accountTagService;
        this.accountTagToAccountTagRequestsConverter = accountTagToAccountTagRequestsConverter;
        this.accountTagToAccountTagResponsesConverter = accountTagToAccountTagResponsesConverter;
        this.accountTagsRequestToAccountTagConverter = accountTagsRequestToAccountTagConverter;
        this.defaultInternalAccountTagService = defaultInternalAccountTagService;
        this.environmentService = environmentService;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public AccountTagResponses list() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return listInAccount(accountId);
    }

    @Override
    @InternalOnly
    public AccountTagResponses listInAccount(String accountId) {
        Set<AccountTag> accountTags = accountTagService.get(accountId);
        List<AccountTagResponse> accountTagResponses = accountTagToAccountTagResponsesConverter.convert(accountTags);
        defaultInternalAccountTagService.merge(accountTagResponses);
        return new AccountTagResponses(new HashSet<>(accountTagResponses));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_WRITE)
    public AccountTagResponses put(@Valid AccountTagRequests request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        List<AccountTag> accountTags = accountTagsRequestToAccountTagConverter.convert(request.getTags());
        defaultInternalAccountTagService.validate(accountTags);
        accountTags = accountTagService.create(accountTags, accountId);
        return new AccountTagResponses(new HashSet<>(accountTagToAccountTagResponsesConverter.convert(accountTags)));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public GeneratedAccountTagResponses generate(@ResourceName String environmentName, @ResourceCrn @TenantAwareParam String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = null;
        if (!Strings.isNullOrEmpty(environmentCrn)) {
            environmentDto = environmentService.getByCrnAndAccountId(environmentCrn, accountId);
        } else if (!Strings.isNullOrEmpty(environmentName)) {
            environmentDto = environmentService.getByNameAndAccountId(environmentName, accountId);
        }
        Tags accountTagsMap = new Tags();
        if (environmentDto != null) {
            accountTagsMap = accountTagService.generate(accountId, environmentDto);
        }
        Set<GeneratedAccountTagResponse> accountTags = new HashSet<>();
        for (Map.Entry<String, String> entry : accountTagsMap.getAll().entrySet()) {
            GeneratedAccountTagResponse accountTag = new GeneratedAccountTagResponse();
            accountTag.setKey(entry.getKey());
            accountTag.setValue(entry.getValue());
            accountTags.add(accountTag);
        }
        return new GeneratedAccountTagResponses(accountTags);
    }
}
