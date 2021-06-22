package com.sequenceiq.mock.verification.intercept;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.mock.service.ResponseModifierService;
import com.sequenceiq.mock.verification.Call;
import com.sequenceiq.mock.verification.RequestResponseStorageService;

@Component
public class EndpointTestDecoratorInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTestDecoratorInterceptor.class);

    @Inject
    private RequestResponseStorageService requestResponseStorageService;

    @Inject
    private ResponseModifierService responseModifierService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestUri = request.getRequestURI();
        Call call = new Call.Builder()
                .uri(requestUri)
                .contentType(request.getContentType())
                .headers(getHeadersMap(request))
                .method(request.getMethod())
                .parameters(getRequestParams(request))
                .postBody(getRequestBody(request))
                .url(request.getRequestURL().toString())
                .build();
        String mockUuid = parseMockUuid(requestUri);
        requestResponseStorageService.put(mockUuid, call);
    }

    private Map<String, String> getRequestParams(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString != null) {
            String[] split = queryString.split("&");
            Map<String, String> params = new HashMap<>();
            Arrays.stream(split).forEach(s -> {
                String decoded = URLDecoder.decode(s, Charset.defaultCharset());
                String[] split1 = decoded.split("=");
                String value = "";
                if (split1.length == 2) {
                    value = split1[1];
                }
                String values = params.computeIfAbsent(split1[0], key -> "");
                if (!StringUtils.isEmpty(value)) {
                    if (!StringUtils.isEmpty(values)) {
                        values += ",";
                    }
                    values += value;
                    params.put(split1[0], values);
                }
            });
            return params;
        }
        return null;
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        if (request instanceof ContentCachingRequestWrapper) {
            return new String(((ContentCachingRequestWrapper) request).getContentAsByteArray());
        } else {
            return "";
        }
    }

    private String parseMockUuid(String requestUri) {
        String toSplit = requestUri;
        if (toSplit.startsWith("/")) {
            toSplit = toSplit.replaceFirst("/", "");
        }
        String[] split = toSplit.split("/");
        if (Crn.isCrn(split[0])) {
            return split[0];
        }
        return requestUri;
    }

    private Map<String, String> getHeadersMap(HttpServletRequest request) {
        Map<String, String> ret = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(s -> {
            String value = request.getHeader(s);
            ret.put(s, value);
        });
        return ret;
    }
}
