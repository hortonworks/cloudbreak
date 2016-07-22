package com.sequenceiq.cloudbreak.shell.transformer;

import javax.ws.rs.ClientErrorException;

import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class ExceptionTransformer {

    public RuntimeException transformToRuntimeException(Exception e) {
        if (e instanceof ClientErrorException) {
            if (((ClientErrorException) e).getResponse() != null && ((ClientErrorException) e).getResponse().hasEntity()) {
                String response = ((ClientErrorException) e).getResponse().readEntity(String.class);
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
