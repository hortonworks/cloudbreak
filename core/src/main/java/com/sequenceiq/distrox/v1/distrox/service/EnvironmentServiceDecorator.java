package com.sequenceiq.distrox.v1.distrox.service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;

@Service
public class EnvironmentServiceDecorator {

    @Inject
    private EnvironmentClientService environmentClientService;

    public void prepareEnvironmentsAndCredentialName(Set<StackViewV4Response> stackViewResponses) {
        Collection<SimpleEnvironmentResponse> responses = environmentClientService.list().getResponses();
        for (StackViewV4Response stackViewResponse : stackViewResponses) {
            Optional<SimpleEnvironmentResponse> first = responses.stream()
                    .filter(x -> x.getCrn().equals(stackViewResponse.getEnvironmentCrn()))
                    .findFirst();
            if (first.isPresent()) {
                stackViewResponse.setCredentialName(first.get().getCredential().getName());
                stackViewResponse.setEnvironmentName(first.get().getName());
            }
        }
    }

    public void prepareEnvironmentAndCredentialName(StackV4Response stackResponse) {
        DetailedEnvironmentResponse byCrn = environmentClientService.getByCrn(stackResponse.getEnvironmentCrn());
        stackResponse.setEnvironmentName(byCrn.getName());
        stackResponse.setCredentialName(byCrn.getCredential().getName());
    }

}
