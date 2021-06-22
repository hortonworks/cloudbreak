package com.sequenceiq.cloudbreak.cm.client.tracing;

import static com.sequenceiq.cloudbreak.client.RequestIdProvider.REQUEST_ID_HEADER;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RequestIdProvider;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

@Component
public class CmRequestIdProviderInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request modifiedRequest = originalRequest.newBuilder()
                .header(REQUEST_ID_HEADER, RequestIdProvider.getOrGenerateRequestId())
                .build();
        return chain.proceed(modifiedRequest);
    }
}
