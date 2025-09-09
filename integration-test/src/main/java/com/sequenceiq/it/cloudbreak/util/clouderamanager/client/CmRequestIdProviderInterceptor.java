package com.sequenceiq.it.cloudbreak.util.clouderamanager.client;

import static com.sequenceiq.cloudbreak.logger.MDCRequestIdOnlyFilter.REQUEST_ID_HEADER;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@Component
public class CmRequestIdProviderInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request modifiedRequest = originalRequest.newBuilder()
                .header(REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId())
                .build();
        return chain.proceed(modifiedRequest);
    }
}
