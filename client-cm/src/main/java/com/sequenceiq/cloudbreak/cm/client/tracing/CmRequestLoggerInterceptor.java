package com.sequenceiq.cloudbreak.cm.client.tracing;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import okio.Buffer;

@Component
public class CmRequestLoggerInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmRequestLoggerInterceptor.class);

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if ("GET".equals(request.method())) {
            return chain.proceed(request);
        }
        LOGGER.debug("[CM request]: url:{}, method:{}, body:{}", request.urlString(), request.method(),
                AnonymizerUtil.anonymize(getBodyAsString(request)));
        Response response = chain.proceed(chain.request());
        LOGGER.debug("[CM response]: statusCode:{}, message:{}, url:{}", response.code(), response.message(), request.urlString());
        return response;
    }

    private String getBodyAsString(Request request) throws IOException {
        if (request.body() == null) {
            return "";
        }
        try (var buffer = new Buffer()) {
            request.body().writeTo(buffer);
            return buffer.readUtf8();
        }
    }
}