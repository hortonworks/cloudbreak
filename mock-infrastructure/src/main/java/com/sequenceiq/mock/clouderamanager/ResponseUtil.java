package com.sequenceiq.mock.clouderamanager;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.WebUtils;

public class ResponseUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseUtil.class);

    private ResponseUtil() {

    }

    public static ResponseEntity noHandlerFoundResponse(HttpServletRequest request) {
        String message = String.format("Interface implemented but the handler is empty for %s %s", request.getMethod(), getRequestUri(request));
        LOGGER.info(message);
        return new ResponseEntity(message, HttpStatus.NOT_FOUND);
    }

    private static String getRequestUri(HttpServletRequest request) {
        String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
        if (uri == null) {
            uri = request.getRequestURI();
        }
        return uri;
    }
}
