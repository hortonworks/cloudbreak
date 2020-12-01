package com.sequenceiq.distrox.v1.distrox.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class SdxServiceDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxServiceDecorator.class);

    @Inject
    private SdxClientService sdxClientService;

    public void prepareMultipleSdxAttributes(Set<StackViewV4Response> stackViewResponses) {
        List<SdxClusterResponse> responses = sdxClientService.list();
        for (StackViewV4Response stackViewResponse : stackViewResponses) {
            Optional<SdxClusterResponse> first = responses.stream()
                    .filter(x -> x.getEnvironmentCrn().equals(stackViewResponse.getEnvironmentCrn()))
                    .findFirst();
            if (first.isPresent()) {
                SdxClusterResponse sdxCluster = first.get();
                SharedServiceV4Response sharedServiceResponse = stackViewResponse.getCluster().getSharedServiceResponse();
                sharedServiceResponse.setSdxCrn(sdxCluster.getCrn());
                sharedServiceResponse.setSdxName(sdxCluster.getName());
            } else {
                LOGGER.info("No SDX cluster found for stack {}.", stackViewResponse.getCrn());
            }
        }
    }

    public void prepareSdxAttributes(StackV4Response stackResponse) {
        getSdxForStack(stackResponse).ifPresent(sdx -> {
            stackResponse.getSharedService().setSdxCrn(sdx.getCrn());
            stackResponse.getSharedService().setSdxName(sdx.getName());
        });
    }

    private Optional<SdxClusterResponse> getSdxForStack(StackV4Response stack) {
        List<SdxClusterResponse> sdxClusters = sdxClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
        if (sdxClusters.size() > 1) {
            LOGGER.warn("More than 1 SDX cluster found for stack {}.", stack.getCrn());
            throw new BadRequestException("Environment should have only one datalake.");
        }
        if (sdxClusters.size() == 1) {
            return Optional.of(sdxClusters.get(0));
        }
        LOGGER.info("No SDX cluster found for stack {}.", stack.getCrn());
        return Optional.empty();
    }
}
