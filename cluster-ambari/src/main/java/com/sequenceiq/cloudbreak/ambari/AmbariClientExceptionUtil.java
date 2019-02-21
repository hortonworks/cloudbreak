package com.sequenceiq.cloudbreak.ambari;


import java.io.Reader;

import org.apache.commons.io.IOUtils;

import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

public class AmbariClientExceptionUtil {

    private AmbariClientExceptionUtil() { }

    public static String getErrorMessage(HttpResponseException e) {
        try {
            String json = IOUtils.toString((Reader) e.getResponse().getData());
            return JsonUtil.readTree(json).get("message").asText();
        } catch (Exception ignored) {
            return "Could not get error cause from exception of Ambari client: " + e;
        }
    }

}
