package com.sequenceiq.it.spark.ambari;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariBlueprintsResponse extends ITResponse {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.putArray("host_groups").addObject()
                    .put("name", "host_group")
                    .putArray("components")
                        .addObject()
                            .put("name", "");
        return rootNode.toString();
    }
}
