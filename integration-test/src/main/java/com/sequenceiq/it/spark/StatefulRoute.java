package com.sequenceiq.it.spark;

import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;

import spark.Request;
import spark.Response;

@FunctionalInterface
public interface StatefulRoute {

    Object handle(Request request, Response response, DefaultModel model) throws Exception;

}