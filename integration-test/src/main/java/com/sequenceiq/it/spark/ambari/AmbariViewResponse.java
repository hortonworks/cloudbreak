package com.sequenceiq.it.spark.ambari;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariViewResponse extends ITResponse {

    private static VelocityEngine ve = new VelocityEngine();

    private static Template ambariViewTeml;

    private String ambariViewJson;

    static {
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
        ambariViewTeml = ve.getTemplate("mockresponse/ambari/ambari-view.json.vm");
    }

    public AmbariViewResponse(String mockServerAddress) {
        VelocityContext c = new VelocityContext();
        c.put("mockServerAddress", mockServerAddress);
        StringWriter sw = new StringWriter();
        ambariViewTeml.merge(c, sw);
        ambariViewJson = sw.toString();
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        return ambariViewJson;
    }

}
