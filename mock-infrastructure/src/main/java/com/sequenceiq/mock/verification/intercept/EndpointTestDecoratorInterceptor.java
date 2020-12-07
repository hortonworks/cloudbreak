package com.sequenceiq.mock.verification.intercept;

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
        String testName = request.getHeader("test-name");
        String requestURI = request.getRequestURI();
        if (!StringUtils.isEmpty(testName)) {
            Call call = new Call.Builder()
                    .uri(requestURI)
                    .contentType(request.getContentType())
                    .headers(getHeadersMap(request))
                    .method(request.getMethod())
//                .parameters()
//                .postBody()
                    .url(request.getRequestURL().toString())
                    .build();
            requestResponseStorageService.put(testName, call);
        }
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
