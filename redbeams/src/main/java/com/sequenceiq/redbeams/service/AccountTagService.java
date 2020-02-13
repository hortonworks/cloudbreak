package com.sequenceiq.redbeams.service;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.api.v1.tags.endpoint.AccountTagEndpoint;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;

@Service
public class AccountTagService {

    @Inject
    private AccountTagEndpoint accountTagEndpoint;

    public Map<String, String> list() {
        AccountTagResponses list = accountTagEndpoint.list();
        return list.getResponses()
                .stream()
                .collect(Collectors.toMap(AccountTagResponse::getKey, AccountTagResponse::getValue));
    }
}
