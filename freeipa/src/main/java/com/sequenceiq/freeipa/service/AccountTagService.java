package com.sequenceiq.freeipa.service;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.tags.endpoint.AccountTagEndpoint;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;

@Service
public class AccountTagService {

    @Inject
    private AccountTagEndpoint accountTagEndpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public Map<String, String> list() {
        try {
            AccountTagResponses list = accountTagEndpoint.list();
            return list.getResponses()
                    .stream()
                    .collect(Collectors.toMap(AccountTagResponse::getKey, AccountTagResponse::getValue));
        } catch (ClientErrorException e) {
            try (Response response = e.getResponse()) {
                if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                    throw new BadRequestException(String.format("Account tag not found"), e);
                }
                String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                throw new CloudbreakServiceException(String.format("Failed to get account tag: %s", errorMessage), e);
            }
        }
    }
}
