package com.sequenceiq.cloudbreak.service.datalake;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class SdxClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxClientService.class);

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Inject
    private SdxInternalEndpoint internalEndpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public List<SdxClusterResponse> getByEnvironmentCrn(String environmentCrn) {
        try {
            return sdxEndpoint.getByEnvCrn(environmentCrn);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            LOGGER.error(String.format("Failed to get datalake clusters for environment %s", environmentCrn), e);
            return new ArrayList<>();
        }
    }

    public List<SdxClusterResponse> getByEnvironmentCrnInernal(String environmentCrn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> sdxEndpoint.getByEnvCrn(environmentCrn));
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            LOGGER.error(String.format("Failed to get datalake clusters for environment %s", environmentCrn), e);
            return new ArrayList<>();
        }
    }

    public SdxClusterResponse getByCrn(String crn) {
        return sdxEndpoint.getByCrn(crn);
    }

    public SdxClusterResponse getByCrnInternal(String crn) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> sdxEndpoint.getByCrn(crn));
    }

    public List<SdxClusterResponse> list() {
        try {
            return sdxEndpoint.list(null, false);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            LOGGER.error("Failed to list datalake clusters.", e);
            return new ArrayList<>();
        }
    }

    public void updateDatabaseEngineVersion(String crn, String databaseEngineVersion) {
        ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> internalEndpoint.updateDbEngineVersion(crn, databaseEngineVersion));
    }
}
