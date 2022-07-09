package com.sequenceiq.it.cloudbreak;

import java.lang.reflect.Field;
import java.util.Set;

import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.idbmms.GrpcIdbmmsClient;
import com.sequenceiq.cloudbreak.idbmms.config.IdbmmsConfig;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.dto.idbmms.IdbmmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

import io.opentracing.Tracer;

public class IdbmmsClient<E extends Enum<E>, W extends WaitObject> extends MicroserviceClient<GrpcIdbmmsClient, Void, E, W> {

    public static final String IDBMMS_CLIENT = "IDBMMS_CLIENT";

    private GrpcIdbmmsClient idbmmsClient;

    IdbmmsClient(String newId) {
        super(newId);
    }

    IdbmmsClient() {
        this(IDBMMS_CLIENT);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        throw new TestFailException("Flow does not support by idbmms client");
    }

    @Override
    public <T extends WaitObject> WaitService<T> waiterService() {
        throw new TestFailException("Wait service does not support by ums client");
    }

    @Override
    public GrpcIdbmmsClient getDefaultClient() {
        return idbmmsClient;
    }

    /**
     * Here we need a valid idbroker mapping management host and port (Manowar Dev), if the default is not correct!
     *
     * @param tracer    Tracer is a simple, thin interface for Span creation and propagation
     * @param host      Manowar Dev idbrokermappingmanagement host
     * @return a connected IdbmmsClient pointing to Manowar Dev
     */
    public static synchronized IdbmmsClient createProxyIdbmmsClient(Tracer tracer, String host) {
        IdbmmsClient clientEntity = new IdbmmsClient();
        IdbmmsConfig clientConfig = new IdbmmsConfig();
        Field endpoint = ReflectionUtils.findField(IdbmmsConfig.class, "endpoint");
        ReflectionUtils.makeAccessible(endpoint);
        ReflectionUtils.setField(endpoint, clientConfig, host);
        Field port = ReflectionUtils.findField(IdbmmsConfig.class, "port");
        ReflectionUtils.makeAccessible(port);
        ReflectionUtils.setField(port, clientConfig, 8990);
        clientEntity.idbmmsClient = GrpcIdbmmsClient.createClient(clientConfig, tracer);
        return clientEntity;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(IdbmmsTestDto.class.getSimpleName());
    }
}
