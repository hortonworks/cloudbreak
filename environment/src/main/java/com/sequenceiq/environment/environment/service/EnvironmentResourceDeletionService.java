package com.sequenceiq.environment.environment.service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.exception.UnableToDeleteClusterDefinitionException;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.experience.ExperienceConnectorService;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sequenceiq.environment.TempConstants.TEMP_WORKSPACE_ID;

@Service
public class EnvironmentResourceDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentResourceDeletionService.class);

    private final SdxEndpoint sdxEndpoint;

    private final DatalakeV4Endpoint datalakeV4Endpoint;

    private final DistroXV1Endpoint distroXV1Endpoint;

    private final ClusterTemplateV4Endpoint clusterTemplateV4Endpoint;

    private final ExperienceConnectorService experienceConnectorService;

    public EnvironmentResourceDeletionService(SdxEndpoint sdxEndpoint, DatalakeV4Endpoint datalakeV4Endpoint, DistroXV1Endpoint distroXV1Endpoint,
                                              ClusterTemplateV4Endpoint clusterTemplateV4Endpoint, ExperienceConnectorService experienceConnectorService) {
        this.sdxEndpoint = sdxEndpoint;
        this.datalakeV4Endpoint = datalakeV4Endpoint;
        this.distroXV1Endpoint = distroXV1Endpoint;
        this.clusterTemplateV4Endpoint = clusterTemplateV4Endpoint;
        this.experienceConnectorService = experienceConnectorService;
    }

    public void deleteClusterDefinitionsOnCloudbreak(String environmentCrn) {
        try {
            clusterTemplateV4Endpoint.deleteMultiple(TEMP_WORKSPACE_ID, null, null, environmentCrn);
        } catch (WebApplicationException e) {
            propagateException("Failed to delete cluster definition(s) from Cloudbreak due to:", e);
        } catch (ProcessingException | UnableToDeleteClusterDefinitionException e) {
            propagateException("Failed to delete cluster definition(s) from Cloudbreak due to:", e);
        }
    }

    public Set<String> getAttachedSdxClusterCrns(Environment environment) {
        Set<String> clusterCrns = new HashSet<>();
        LOGGER.debug("Get SDX clusters of the environment: '{}'", environment.getName());
        try {
            Set<String> sdxClusterCrns = sdxEndpoint
                    .list(environment.getName())
                    .stream()
                    .map(SdxClusterResponse::getCrn)
                    .collect(Collectors.toSet());
            clusterCrns.addAll(sdxClusterCrns);
        } catch (WebApplicationException e) {
            propagateException("Failed to get SDX clusters from SDX service due to:", e);
        } catch (ProcessingException e) {
            propagateException("Failed to get SDX clusters from SDX service due to:", e);
        }
        return clusterCrns;
    }

    public Set<String> getDatalakeClusterNames(Environment environment) {
        Set<String> clusterNames = new HashSet<>();
        LOGGER.debug("Get Datalake clusters of the environment: '{}'", environment.getName());
        try {
            Set<String> datalakeClusterNames = datalakeV4Endpoint
                    .list(null, environment.getResourceCrn())
                    .getResponses()
                    .stream()
                    .map(StackViewV4Response::getName)
                    .collect(Collectors.toSet());
            clusterNames.addAll(datalakeClusterNames);
        } catch (WebApplicationException e) {
            propagateException("Failed to get Datalake clusters from Cloudbreak service due to:", e);
        } catch (ProcessingException e) {
            propagateException("Failed to get Datalake clusters from Cloudbreak service due to:", e);
        }
        return clusterNames;
    }

    public long getConnectedExperienceAmount(Environment environment) {
        return experienceConnectorService.getConnectedExperienceQuantity(environment);
    }

    Set<String> getAttachedDistroXClusterNames(Environment environment) {
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
