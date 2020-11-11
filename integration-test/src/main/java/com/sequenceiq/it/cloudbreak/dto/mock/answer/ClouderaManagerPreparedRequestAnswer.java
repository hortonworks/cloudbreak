package com.sequenceiq.it.cloudbreak.dto.mock.answer;


import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

import spark.Request;

public class ClouderaManagerPreparedRequestAnswer<S, T> extends AbstractRequestWithBodyHandler<S, T, ClouderaManagerPreparedRequestAnswer<S, T>> {

    public ClouderaManagerPreparedRequestAnswer(Method method, String path, Class<T> requestType, HttpMock mock,
            ExecuteQueryToMockInfrastructure executeQuery) {
        super(method, path, requestType, mock, executeQuery);
    }

    T prepareRequestInstance(Request request) {
        ApiClient client = new ApiClient();
        return client.getJSON().deserialize(request.body(), getRequestType());
    }
}
