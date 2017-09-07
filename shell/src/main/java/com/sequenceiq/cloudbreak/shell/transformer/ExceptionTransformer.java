package com.sequenceiq.cloudbreak.shell.transformer;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class ExceptionTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionTransformer.class);

    public RuntimeException transformToRuntimeException(Exception e) {
        LOGGER.error("Transform exception: {}", e.getMessage(), e);
        if (e instanceof ClientErrorException) {
            Response webAppResponse = ((WebApplicationException) e).getResponse();
            if (webAppResponse != null && webAppResponse.hasEntity()) {
                String response = webAppResponse.readEntity(String.class);
                if (response != null) {
                    String[] split = response.replaceAll("}", "").replaceAll("\\{", "").replaceAll("\\\\\"", "'").split("\"");
                    String splitResponse = split[split.length - 1];
                    if (StringUtils.isEmpty(response)) {
                        return new RuntimeException(response);
                    } else {
                        return new RuntimeException(splitResponse);
                    }
                }
            }
        }
        return new RuntimeException(e.getMessage());
    }

    public RuntimeException transformToRuntimeException(String errorMessage) {
        return new RuntimeException(errorMessage);
    }
}
