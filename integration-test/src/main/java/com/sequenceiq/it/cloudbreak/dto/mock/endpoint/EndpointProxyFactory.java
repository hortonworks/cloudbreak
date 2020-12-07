package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import java.lang.reflect.Proxy;

import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.dto.mock.SparkUriAnnotationHandler;
import com.sequenceiq.it.cloudbreak.dto.mock.SparkUriParameters;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

public class EndpointProxyFactory {

    private EndpointProxyFactory() {

    }

    public static <E extends VerificationEndpoint<T>, T extends CloudbreakTestDto> E create(Class<E> endpoint, T testDto,
            ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure) {
        return (E) Proxy.newProxyInstance(
                testDto.getClass().getClassLoader(),
                new Class[]{endpoint},
                (proxy, method, args) -> {
                    Method httpMethod = Method.build(method.getName());
                    SparkUriParameters parameters = new SparkUriAnnotationHandler(endpoint, method).getParameters();
                    return new DefaultResponseConfigure(httpMethod, parameters.getUri(), testDto, executeQueryToMockInfrastructure);

                });
    }
}
