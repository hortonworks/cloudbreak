package com.sequenceiq.cloudbreak.logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.logger.rest.RestLoggerField;

public class RestLoggerFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DateFormat formatter;

    private final boolean restLoggerEnabled;

    public RestLoggerFilter(boolean restLoggerEnabled) {
        this.restLoggerEnabled = restLoggerEnabled;
        this.formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        this.formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Date start = new Date(System.currentTimeMillis());
        ContentCachingRequestWrapper wrappedRequest = getWrappedRequest(request);
        ContentCachingResponseWrapper wrappedResponse = getWrappedResponse(response);

        filterChain.doFilter(wrappedRequest, wrappedResponse);

        if (restLoggerEnabled) {
            Date end = new Date(System.currentTimeMillis());
            String log = new StringBuilder()
                    .append(appendLine(RestLoggerField.START_TIME, formatter.format(start)))
                    .append(appendLine(RestLoggerField.END_TIME, formatter.format(end)))
                    .append(appendLine(RestLoggerField.DURATION, Math.abs(end.getTime() - start.getTime()) + " ms"))
                    .append(appendLine(RestLoggerField.HTTP_METHOD, request.getMethod()))
                    .append(appendLine(RestLoggerField.PATH, request.getRequestURI()))
                    .append(appendLine(RestLoggerField.QUERY_STRING, request.getQueryString()))
                    .append(appendLine(RestLoggerField.CLIENT_IP, request.getRemoteAddr()))
                    .append(appendLine(RestLoggerField.REQUEST,
                            logContent(wrappedRequest.getContentAsByteArray(), request.getCharacterEncoding())))
                    .append(appendLine(RestLoggerField.RESPONSE_STATUS, String.valueOf(response.getStatus())))
                    .append(appendLine(RestLoggerField.RESPONSE,
                            logContent(wrappedResponse.getContentAsByteArray(), request.getCharacterEncoding())))
                    .toString();
            logger.debug(log);
        }
        wrappedResponse.copyBodyToResponse();
    }

    @NotNull
    private ContentCachingResponseWrapper getWrappedResponse(HttpServletResponse response) {
        return new ContentCachingResponseWrapper(response);
    }

    @NotNull
    private ContentCachingRequestWrapper getWrappedRequest(HttpServletRequest request) {
        return new ContentCachingRequestWrapper(request);
    }

    private static String logContent(byte[] content, String contentEncoding) {
        String contentString;
        try {
            contentString = new String(content, contentEncoding);
        } catch (UnsupportedEncodingException e) {
            contentString = "";
        }
        return AnonymizerUtil.anonymize(contentString);
    }

    private String appendLine(RestLoggerField key, String value) {
        return String.format("%s: %s %s", key.field(), value, "\n");
    }
}