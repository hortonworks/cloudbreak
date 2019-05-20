package com.sequenceiq.it.cloudbreak.spark;

import com.sequenceiq.it.cloudbreak.mock.DefaultModel;

import spark.Request;
import spark.Response;

@FunctionalInterface
public interface StatefulRoute {

    Object handle(Request request, Response response, DefaultModel model) throws Exception;

}