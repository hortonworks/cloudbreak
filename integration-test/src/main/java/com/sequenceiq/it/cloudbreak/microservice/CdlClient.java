package com.sequenceiq.it.cloudbreak.microservice;

import java.util.Set;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.cdl.config.SdxCdlChannelConfig;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.dto.cdl.CdlTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

public class CdlClient<E extends Enum<E>, W extends WaitObject> extends MicroserviceClient<GrpcSdxCdlClient, Void, E, W> {

    private GrpcSdxCdlClient cdlClient;

    public CdlClient(String host, int port, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        cdlClient = GrpcSdxCdlClient.createClient(SdxCdlChannelConfig.newManagedChannelWrapper(host, port), regionAwareInternalCrnGeneratorFactory);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        throw new TestFailException("Flow does not support by cdl client");
    }

    @Override
    public <T extends WaitObject> WaitService<T> waiterService() {
        throw new TestFailException("Wait service does not support by cdl client");
    }

    @Override
    public GrpcSdxCdlClient getDefaultClient() {
        return cdlClient;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(CdlTestDto.class.getSimpleName());
    }
}
