package com.sequenceiq.distrox.v1.distrox.service;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialViewResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;

@Service
public class EnvironmentServiceDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentServiceDecorator.class);

    @Inject
    private EnvironmentService environmentClientService;

    public void prepareEnvironmentsAndCredentialName(Set<StackViewV4Response> stackViewResponses, NameOrCrn nameOrCrn) {
        if (nameOrCrn.hasCrn()) {
            prepareEnvironmentAndCredential(stackViewResponses, environmentClientService.getByCrn(nameOrCrn.getCrn()));
        } else if (nameOrCrn.hasName()) {
            prepareEnvironmentAndCredential(stackViewResponses, environmentClientService.getByName(nameOrCrn.getName()));
        } else {
            Collection<SimpleEnvironmentResponse> responses = environmentClientService.list().getResponses();
            for (StackViewV4Response stackViewResponse : stackViewResponses) {
                Optional<SimpleEnvironmentResponse> first = responses.stream()
                        .filter(x -> x.getCrn().equals(stackViewResponse.getEnvironmentCrn()))
                        .findFirst();
                if (first.isPresent()) {
                    CredentialViewResponse credential = first.get().getCredential();
                    stackViewResponse.setCredentialName(credential.getName());
                    stackViewResponse.setGovCloud(credential.getGovCloud() == null ? false : credential.getGovCloud());
                    stackViewResponse.setEnvironmentName(first.get().getName());
                }
            }
        }

    }

    public void prepareEnvironmentAndCredential(Set<StackViewV4Response> stackViewResponses,
                                                DetailedEnvironmentResponse detailedEnvironmentResponse) {
        for (StackViewV4Response stackViewResponse : stackViewResponses) {
            if (detailedEnvironmentResponse != null) {
                CredentialResponse credential = detailedEnvironmentResponse.getCredential();
                stackViewResponse.setGovCloud(credential.getGovCloud() == null ? false : credential.getGovCloud());
                stackViewResponse.setCredentialName(credential.getName());
                stackViewResponse.setEnvironmentName(detailedEnvironmentResponse.getName());
            }
        }
    }

    public void prepareEnvironmentAndCredentialName(StackV4Response stackResponse) {
        try {
            DetailedEnvironmentResponse byCrn = environmentClientService.getByCrn(stackResponse.getEnvironmentCrn());
            stackResponse.setEnvironmentName(byCrn.getName());
            CredentialResponse credential = byCrn.getCredential();
            stackResponse.setGovCloud(credential.getGovCloud() == null ? false : credential.getGovCloud());
            stackResponse.setCredentialName(credential.getName());
            stackResponse.setCredentialCrn(credential.getCrn());
            stackResponse.setEnvironmentType(byCrn.getEnvironmentType());
        } catch (Exception e) {
            LOGGER.warn("Environment deleted which had crn: {}.", stackResponse.getEnvironmentCrn());
        }
    }

    public void prepareEnvironments(Set<ClusterTemplateViewV4Response> clusterTemplateViewV4Responses) {
        LOGGER.debug("Decorating with environment name the following cluster definition(s): {}",
                clusterTemplateViewV4Responses.stream().map(CompactViewV4Response::getName).collect(toSet()));
        Collection<SimpleEnvironmentResponse> responses = environmentClientService.list().getResponses();
        for (ClusterTemplateViewV4Response clusterTemplateViewV4Response : clusterTemplateViewV4Responses) {
            Optional<SimpleEnvironmentResponse> first = responses.stream()
                    .filter(x -> x.getCrn().equals(clusterTemplateViewV4Response.getEnvironmentCrn()))
                    .findFirst();
            first.ifPresentOrElse(simpleEnvironmentResponse -> clusterTemplateViewV4Response.setEnvironmentName(simpleEnvironmentResponse.getName()), () ->
                    LOGGER.info("Unable to find environment name for cluster definition \"{}\"", clusterTemplateViewV4Response.getName()));
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
