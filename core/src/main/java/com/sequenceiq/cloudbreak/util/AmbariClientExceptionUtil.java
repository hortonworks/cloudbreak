package com.sequenceiq.cloudbreak.util;

import java.io.StringReader;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import groovyx.net.http.HttpResponseException;


public class AmbariClientExceptionUtil {

    private AmbariClientExceptionUtil() { }

    public static String getErrorMessage(HttpResponseException e) {
        try {
            String json = IOUtils.toString((StringReader) e.getResponse().getData());
            return new ObjectMapper().readTree(json).get("message").asText();
        } catch (Exception ex) {
            return "Could not get error cause from exception of Ambari client: " + e.toString();
        }
    }

}
