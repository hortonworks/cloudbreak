package com.sequenceiq.it.cloudbreak.mock.ambari;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariComponentStatusOnHostResponse extends ITResponse {

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        ObjectNode roles = rootNode.putObject("HostRoles");
        roles.put("desired_admin_state", "INSERVICE");
        roles.put("desired_state", "STARTED");
        roles.put("maintenance_state", "OFF");
        roles.put("state", "STARTED");
        roles.put("upgrade_state", "NONE");

        return rootNode;
    }
}