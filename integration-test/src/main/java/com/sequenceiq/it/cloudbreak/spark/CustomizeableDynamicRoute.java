package com.sequenceiq.it.cloudbreak.spark;

import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ContainerNode;
import com.sequenceiq.it.cloudbreak.Mock;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;

import spark.Request;
import spark.Response;
import spark.Route;

public class CustomizeableDynamicRoute implements Route {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomizeableDynamicRoute.class);

    private Mode mode;

    private Stack<Route> simpleRouteImplementation = new Stack<>();

    private Stack<StatefulRoute> routeImplementation = new Stack<>();

    private DefaultModel model;

    public CustomizeableDynamicRoute(Route simpleRouteImplementation) {
        this.simpleRouteImplementation.push(simpleRouteImplementation);
        mode = Mode.SIMPLE;
    }

    public CustomizeableDynamicRoute(StatefulRoute routeImplementation, DefaultModel model) {
        this.routeImplementation.push(routeImplementation);
        mode = Mode.STATEFUL;
    }

    public void setRouteImplementation(StatefulRoute routeImplementation, DefaultModel model) {
        this.routeImplementation.push(routeImplementation);
        this.model = model;
        mode = Mode.STATEFUL;
    }

    public void setSimpleRouteImplementation(Route simpleRouteImplementation) {
        this.simpleRouteImplementation.push(simpleRouteImplementation);
        mode = Mode.SIMPLE;
    }

    public void clearRouteImpls() {
        routeImplementation.clear();
        simpleRouteImplementation.clear();
        mode = Mode.INVALID;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        if (mode.equals(Mode.INVALID)) {
            throw new RuntimeException("Route is in INVALID state, probably got cleared but not reinitialized");
        }
        Object handle;
        if (mode.equals(Mode.STATEFUL)) {
            handle = getSafeStateful().handle(request, response, model);
        } else {
            handle = getSafeSimple().handle(request, response);
        }
        if (handle instanceof String) {
            return handle;
        } else if (handle instanceof ContainerNode) {
            return handle.toString();
        } else {
            return Mock.gson().toJson(handle);
        }
    }

    private Route getSafeSimple() {
        if (simpleRouteImplementation.size() > 1) {
            return simpleRouteImplementation.pop();
        } else {
            return simpleRouteImplementation.peek();
        }
    }

    private StatefulRoute getSafeStateful() {
        if (routeImplementation.size() > 1) {
            return routeImplementation.pop();
        } else {
            return routeImplementation.peek();
        }
    }

    enum Mode {
        SIMPLE,
        STATEFUL,
        INVALID
    }
}
