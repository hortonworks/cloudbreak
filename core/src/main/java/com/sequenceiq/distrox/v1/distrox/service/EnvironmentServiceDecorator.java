package com.sequenceiq.distrox.v1.distrox.service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;

@Service
public class EnvironmentServiceDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentServiceDecorator.class);

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
        try {
            DetailedEnvironmentResponse byCrn = environmentClientService.getByCrn(stackResponse.getEnvironmentCrn());
            stackResponse.setEnvironmentName(byCrn.getName());
            stackResponse.setCredentialName(byCrn.getCredential().getName());
            stackResponse.setCredentialCrn(byCrn.getCredential().getCrn());
        } catch (Exception e) {
            LOGGER.warn("Environment deleted which had crn: {}.", stackResponse.getEnvironmentCrn());
        }
    }

    public void prepareEnvironments(Set<ClusterTemplateViewV4Response> clusterTemplateViewV4Responses) {
        Collection<SimpleEnvironmentResponse> responses = environmentClientService.list().getResponses();
        for (ClusterTemplateViewV4Response clusterTemplateViewV4Response : clusterTemplateViewV4Responses) {
            Optional<SimpleEnvironmentResponse> first = responses.stream()
                    .filter(x -> x.getCrn().equals(clusterTemplateViewV4Response.getEnvironmentCrn()))
                    .findFirst();
            if (first.isPresent()) {
                clusterTemplateViewV4Response.setEnvironmentName(first.get().getName());
            }
        }
    }

    public void prepareEnvironment(ClusterTemplateV4Response clusterTemplateV4Response) {
        try {
            DetailedEnvironmentResponse byCrn = environmentClientService.getByCrn(clusterTemplateV4Response.getEnvironmentCrn());
            clusterTemplateV4Response.setEnvironmentName(byCrn.getName());
        } catch (Exception e) {
            LOGGER.warn("Environment deleted which had crn: {}.", clusterTemplateV4Response.getEnvironmentCrn());
        }
    }

    public void prepareEnvironment(StackViewV4Response stackViewResponse) {
        try {
            DetailedEnvironmentResponse byCrn = environmentClientService.getByCrn(stackViewResponse.getEnvironmentCrn());
            stackViewResponse.setEnvironmentName(byCrn.getName());
        } catch (Exception e) {
            LOGGER.warn("Environment deleted which had crn: {}.", stackViewResponse.getEnvironmentCrn());
        }
    }

}
