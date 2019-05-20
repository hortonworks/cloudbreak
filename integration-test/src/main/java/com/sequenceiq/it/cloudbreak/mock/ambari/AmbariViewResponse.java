package com.sequenceiq.it.cloudbreak.mock.ambari;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.sequenceiq.it.cloudbreak.mock.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariViewResponse extends ITResponse {

    private static final VelocityEngine VE = new VelocityEngine();

    private static final Template TEMPLATE;

    private final String ambariViewJson;

    static {
        VE.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        VE.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        VE.init();
        TEMPLATE = VE.getTemplate("mockresponse/ambari/ambari-view.json.vm");
    }

    public AmbariViewResponse(String mockServerAddress) {
        Context c = new VelocityContext();
        c.put("mockServerAddress", mockServerAddress);
        StringWriter sw = new StringWriter();
        TEMPLATE.merge(c, sw);
        ambariViewJson = sw.toString();
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        return ambariViewJson;
    }

}
