package com.sequenceiq.it.cloudbreak.microservice;

import java.util.Set;

import com.sequenceiq.cloudbreak.idbmms.GrpcIdbmmsClient;
import com.sequenceiq.cloudbreak.idbmms.config.IdbmmsChannelConfig;
import com.sequenceiq.cloudbreak.idbmms.config.IdbmmsClientConfig;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.dto.idbmms.IdbmmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

public class IdbmmsClient<E extends Enum<E>, W extends WaitObject> extends MicroserviceClient<GrpcIdbmmsClient, Void, E, W> {

    private GrpcIdbmmsClient idbmmsClient;

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        throw new TestFailException("Flow does not support by idbmms client");
    }

    @Override
    public <T extends WaitObject> WaitService<T> waiterService() {
        throw new TestFailException("Wait service does not support by idbmms client");
    }

    @Override
    public GrpcIdbmmsClient getDefaultClient() {
        return idbmmsClient;
    }

    /**
     * Creates the IDBroker mapping client for test project.
     *
     * @param idbmmsHost    IDBroker Mapping service host
     * @param idbmmsPort    IDBroker Mapping service port
     * @return a connected IdbmmsClient
     */
    public static synchronized IdbmmsClient createIdbmmsClient(String idbmmsHost, int idbmmsPort) {
        IdbmmsClient clientEntity = new IdbmmsClient();
        clientEntity.idbmmsClient = GrpcIdbmmsClient.createClient(
                IdbmmsChannelConfig.newManagedChannelWrapper(idbmmsHost, idbmmsPort), new IdbmmsClientConfig("cloudbreak"));
        return clientEntity;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(IdbmmsTestDto.class.getSimpleName());
    }
}
