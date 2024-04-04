package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAsInternalActor;
import static com.sequenceiq.environment.TempConstants.TEMP_WORKSPACE_ID;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

@Service
public class EnvironmentResourceDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentResourceDeletionService.class);

    private final PlatformAwareSdxConnector platformAwareSdxConnector;

    private final DatalakeV4Endpoint datalakeV4Endpoint;

    private final DistroXV1Endpoint distroXV1Endpoint;

    private final ClusterTemplateV4Endpoint clusterTemplateV4Endpoint;

    private final ExperienceConnectorService experienceConnectorService;

    private final ExternalizedComputeService externalizedComputeService;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public EnvironmentResourceDeletionService(PlatformAwareSdxConnector platformAwareSdxConnector, DatalakeV4Endpoint datalakeV4Endpoint,
            DistroXV1Endpoint distroXV1Endpoint, ClusterTemplateV4Endpoint clusterTemplateV4Endpoint, ExperienceConnectorService experienceConnectorService,
            ExternalizedComputeService externalizedComputeService, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.platformAwareSdxConnector = platformAwareSdxConnector;
        this.datalakeV4Endpoint = datalakeV4Endpoint;
        this.distroXV1Endpoint = distroXV1Endpoint;
        this.clusterTemplateV4Endpoint = clusterTemplateV4Endpoint;
        this.experienceConnectorService = experienceConnectorService;
        this.externalizedComputeService = externalizedComputeService;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public void deleteClusterDefinitionsOnCloudbreak(String environmentCrn) {
        try {
            clusterTemplateV4Endpoint.deleteMultiple(TEMP_WORKSPACE_ID, new HashSet<>(), null, environmentCrn);
        } catch (WebApplicationException e) {
            propagateException("Failed to delete cluster definition(s) from Cloudbreak due to:", e);
        } catch (ProcessingException e) {
            propagateException("Failed to delete cluster definition(s) from Cloudbreak due to:", e);
        }
    }

    public Set<String> getAttachedSdxClusterCrns(EnvironmentView environment) {
        Set<String> clusterCrns = new HashSet<>();
        LOGGER.debug("Get SDX clusters of the environment: '{}'", environment.getName());
        try {
            clusterCrns = platformAwareSdxConnector.listSdxCrns(environment.getName(), environment.getResourceCrn());
        } catch (WebApplicationException e) {
            propagateException("Failed to get SDX clusters from SDX service due to:", e);
        } catch (ProcessingException e) {
            propagateException("Failed to get SDX clusters from SDX service due to:", e);
        }
        return clusterCrns;
    }

    public Set<String> getComputeClusterNames(EnvironmentView environment) {
        Set<String> clusterNames = Set.of();
        try {
            clusterNames = externalizedComputeService.getComputeClusterNames(environment);
        } catch (WebApplicationException | ProcessingException e) {
            propagateException("Failed to get Compute clusters due to:", e);
        }
        return clusterNames;
    }

    public Set<String> getDatalakeClusterNames(EnvironmentView environment) {
        Set<String> clusterNames = new HashSet<>();
        LOGGER.debug("Get Datalake clusters of the environment: '{}'", environment.getName());
        try {
            Set<String> datalakeClusterNames = doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> datalakeV4Endpoint
                            .list(null, environment.getResourceCrn())
                            .getResponses()
                            .stream()
                            .map(StackViewV4Response::getName)
                            .collect(Collectors.toSet()));
            clusterNames.addAll(datalakeClusterNames);
        } catch (WebApplicationException e) {
            propagateException("Failed to get Datalake clusters from Cloudbreak service due to:", e);
        } catch (ProcessingException e) {
            propagateException("Failed to get Datalake clusters from Cloudbreak service due to:", e);
        }
        return clusterNames;
    }

    public int getConnectedExperienceAmount(EnvironmentView environment) {
        EnvironmentExperienceDto dto = EnvironmentExperienceDto.builder()
                .withName(environment.getName())
                .withCrn(environment.getResourceCrn())
                .withAccountId(environment.getAccountId())
                .withCloudPlatform(environment.getCloudPlatform())
                .build();
        return experienceConnectorService.getConnectedExperienceCount(dto);
    }

    Set<String> getAttachedDistroXClusterNames(EnvironmentView environment) {
        Set<String> clusterNames = new HashSet<>();
        LOGGER.debug("Get DistroX clusters of the environment: '{}'", environment.getName());
        try {
            Set<String> distroXClusterNames = distroXV1Endpoint
                    .list(null, environment.getResourceCrn())
                    .getResponses()
                    .stream()
                    .map(StackViewV4Response::getName)
                    .collect(Collectors.toSet());
            clusterNames.addAll(distroXClusterNames);
        } catch (WebApplicationException e) {
            propagateException("Failed to get DistroX clusters from Cloudbreak service due to:", e);
        } catch (ProcessingException e) {
            propagateException("Failed to get DistroX clusters from Cloudbreak service due to:", e);
        }
        return clusterNames;
    }

    private void propagateException(String messagePrefix, WebApplicationException e) {
        String responseMessage = e.getResponse().readEntity(String.class);
        String message = String.format("%s %s. %s", messagePrefix, e.getMessage(), responseMessage);
        throwServiceException(e, message);
    }

    private void propagateException(String messagePrefix, Exception e) {
        String message = String.format("%s '%s' ", messagePrefix, e.getMessage());
        throwServiceException(e, message);
    }

    private void throwServiceException(Exception e, String message) {
        LOGGER.error(message, e);
        throw new EnvironmentServiceException(message, e);
    }

}
