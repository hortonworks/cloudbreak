package com.sequenceiq.environment.proxy.service;

import java.util.List;
import java.util.Objects;

import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class ProxyConfigModificationService {

    private final FreeIpaService freeIpaService;

    private final SdxService sdxService;

    private final DatahubService datahubService;

    public ProxyConfigModificationService(
            FreeIpaService freeIpaService,
            SdxService sdxService,
            DatahubService datahubService) {
        this.freeIpaService = freeIpaService;
        this.sdxService = sdxService;
        this.datahubService = datahubService;
    }

    public boolean shouldModify(Environment environment, ProxyConfig newProxyConfig) {
        ProxyConfig currentProxyConfig = environment.getProxyConfig();
        boolean addProxy = newProxyConfig.getName() != null && currentProxyConfig == null;
        boolean removeProxy = newProxyConfig.getName() == null && currentProxyConfig != null;
        boolean changeProxy = currentProxyConfig != null && !Objects.equals(newProxyConfig.getName(), currentProxyConfig.getName());
        return addProxy || removeProxy || changeProxy;
    }

    public void validateModify(EnvironmentDto environment) {
        DescribeFreeIpaResponse freeIpaResponse = freeIpaService.describe(environment.getResourceCrn())
                .orElseThrow(() -> new IllegalStateException("FreeIpa not found for environment " + environment.getResourceCrn()));
        if (!freeIpaResponse.getStatus().isAvailable()) {
            throw new BadRequestException("Modifying proxy config is not supported when FreeIpa is not available");
        }
        List<SdxClusterResponse> sdxClusters = sdxService.list(environment.getResourceCrn());
        if (sdxClusters.stream().anyMatch(sdxClusterResponse -> !sdxClusterResponse.getStatus().isRunning())) {
            throw new BadRequestException("Modifying proxy config is not supported when Data Lake is not running");
        }
        StackViewV4Responses stackViewV4Responses = datahubService.list(environment.getResourceCrn());
        if (stackViewV4Responses.getResponses().stream().anyMatch(stackViewV4Response -> !stackViewV4Response.getStatus().isAvailable())) {
            throw new BadRequestException("Modifying proxy config is not supported when any of the Data Hubs are not available");
        }
    }
}
