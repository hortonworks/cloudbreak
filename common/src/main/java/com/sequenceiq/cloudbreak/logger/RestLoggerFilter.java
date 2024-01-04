package com.sequenceiq.cloudbreak.logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;

public class RestLoggerFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestLoggerFilter.class);

    private static final int MAX_SIZE = 30000;

    private static final String REDACTED = "REDACTED COMPLETELY";

    private final boolean restLoggerEnabled;

    public RestLoggerFilter(boolean restLoggerEnabled) {
        this.restLoggerEnabled = restLoggerEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Date start = new Date(System.currentTimeMillis());
        ContentCachingRequestWrapper wrappedRequest = getWrappedRequest(request);
        ContentCachingResponseWrapper wrappedResponse = getWrappedResponse(response);

        filterChain.doFilter(wrappedRequest, wrappedResponse);

        String requestPath = request.getRequestURI();
        if (restLoggerEnabled && !excludePathPattern(requestPath)) {
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date end = new Date(System.currentTimeMillis());
            String log = new StringBuilder(MAX_SIZE)
                    .append(appendLine(RestLoggerField.START_TIME, formatter.format(start)))
                    .append(appendLine(RestLoggerField.END_TIME, formatter.format(end)))
                    .append(appendLine(RestLoggerField.DURATION, Math.abs(end.getTime() - start.getTime()) + " ms"))
                    .append(appendLine(RestLoggerField.HTTP_METHOD, request.getMethod()))
                    .append(appendLine(RestLoggerField.PATH, request.getRequestURI()))
                    .append(appendLine(RestLoggerField.QUERY_STRING, request.getQueryString()))
                    .append(appendLine(RestLoggerField.CLIENT_IP, request.getRemoteAddr()))
                    .append(appendLine(RestLoggerField.REQUEST,
                            logContent(wrappedRequest.getContentAsByteArray(), request.getCharacterEncoding(), isUberSensitive(requestPath))))
                    .append(appendLine(RestLoggerField.RESPONSE_STATUS, String.valueOf(response.getStatus())))
                    .append(appendLine(RestLoggerField.RESPONSE,
                            logContent(wrappedResponse.getContentAsByteArray(), request.getCharacterEncoding(), isUberSensitive(requestPath))))
                    .toString();
            LOGGER.debug(log);
        }
        wrappedResponse.copyBodyToResponse();
    }

    private boolean excludePathPattern(String requestPath) {
        return requestPath.contains("/metrics") || requestPath.contains("/autoscale");
    }

    private boolean isUberSensitive(String requestPath) {
        return requestPath.contains("/credential");
    }

    private ContentCachingResponseWrapper getWrappedResponse(HttpServletResponse response) {
        return new ContentCachingResponseWrapper(response);
    }

    private ContentCachingRequestWrapper getWrappedRequest(HttpServletRequest request) {
        return new ContentCachingRequestWrapper(request);
    }

    private String logContent(byte[] content, String contentEncoding, boolean hide) {
        String contentString;
        try {
            contentString = new String(content, contentEncoding);
            if (contentString.length() > MAX_SIZE) {
                contentString = "WARNING - The response size is too large to log.";
            }
        } catch (UnsupportedEncodingException e) {
            contentString = "We were not able to encode the content: " + e.getMessage();
        }
        return redact(contentString, hide);
    }

    private String redact(String contentString, boolean hide) {
        String redacted;
        if (hide) {
            redacted = REDACTED;
        } else {
            redacted = AnonymizerUtil.anonymize(contentString);
        }
        return redacted;
    }

    private String appendLine(RestLoggerField key, String value) {
        String returnValue = String.format("%s: %s %s", key.field(), value, "\n");
        if (returnValue.length() <= MAX_SIZE) {
            return returnValue;
        }
        return "";
    }
}
