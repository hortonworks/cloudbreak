package com.sequenceiq.it.cloudbreak.spark;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpMethod;

import com.sequenceiq.it.cloudbreak.mock.DefaultModel;

import spark.Route;
import spark.Service;

public class DynamicRouteStack {

    private Map<RouteKey, CustomizeableDynamicRoute> mockResponders = new HashMap<>();

    private Service service;

    private DefaultModel model;

    public DynamicRouteStack(Service service, DefaultModel model) {
        this.service = service;
        this.model = model;
    }

    public Route get(String url, Route responseHandler) {
        RouteKey key = new RouteKey(HttpMethod.GET, url);
        boolean hasSparkRoute = mockResponders.containsKey(key);
        Route route = overrideResponseByUrlWithSimple(key, responseHandler);
        addGetIfNotPresent(url, hasSparkRoute, route);
        return route;
    }

    public Route put(String url, Route responseHandler) {
        RouteKey key = new RouteKey(HttpMethod.PUT, url);
        boolean hasSparkRoute = mockResponders.containsKey(key);
        Route route = overrideResponseByUrlWithSimple(key, responseHandler);
        addPutIfNotPresent(url, hasSparkRoute, route);
        return route;
    }

    public Route post(String url, Route responseHandler) {
        RouteKey key = new RouteKey(HttpMethod.POST, url);
        boolean hasSparkRoute = mockResponders.containsKey(key);
        Route route = overrideResponseByUrlWithSimple(key, responseHandler);
        addPostIfNotPresent(url, hasSparkRoute, route);
        return route;
    }

    public Route delete(String url, Route responseHandler) {
        RouteKey key = new RouteKey(HttpMethod.DELETE, url);
        boolean hasSparkRoute = mockResponders.containsKey(key);
        Route route = overrideResponseByUrlWithSimple(key, responseHandler);
        addDeleteIfNotPresent(url, hasSparkRoute, route);
        return route;
    }

    public Route route(HttpMethod method, String url, StatefulRoute responseHandler) {
        RouteKey key = new RouteKey(method, url);
        boolean hasSparkRoute = mockResponders.containsKey(key);
        Route route = overrideResponseByUrlWithStateful(key, responseHandler);
        addRouteIfNotPresent(method, url, hasSparkRoute, route);
        return route;
    }

    public Route get(String url, StatefulRoute responseHandler) {
        return route(HttpMethod.GET, url, responseHandler);
    }

    public Route put(String url, StatefulRoute responseHandler) {
        return route(HttpMethod.PUT, url, responseHandler);
    }

    public Route post(String url, StatefulRoute responseHandler) {
        return route(HttpMethod.POST, url, responseHandler);
    }

    public Route delete(String url, StatefulRoute responseHandler) {
        return route(HttpMethod.DELETE, url, responseHandler);
    }

    public void clear(HttpMethod method, String url) {
        RouteKey key = new RouteKey(method, url);
        clearRouteIfPresent(key);
    }

    public void clearGet(String url) {
        RouteKey key = new RouteKey(HttpMethod.GET, url);
        clearRouteIfPresent(key);
    }

    public void clearPut(String url) {
        RouteKey key = new RouteKey(HttpMethod.PUT, url);
        clearRouteIfPresent(key);
    }

    public void clearPost(String url) {
        RouteKey key = new RouteKey(HttpMethod.POST, url);
        clearRouteIfPresent(key);
    }

    public void clearDelete(String url) {
        RouteKey key = new RouteKey(HttpMethod.DELETE, url);
        clearRouteIfPresent(key);
    }

    private void addRouteIfNotPresent(HttpMethod method, String url, boolean hasSparkRoute, Route route) {
        if (!hasSparkRoute) {
            switch (method) {
                case GET:
                    service.get(url, route);
                    break;
                case PUT:
                    service.put(url, route);
                    break;
                case POST:
                    service.post(url, route);
                    break;
                case DELETE:
                    service.delete(url, route);
                    break;
                case HEAD:
                    service.head(url, route);
                    break;
                default:
                    throw new IllegalArgumentException("http method is not implemented");
            }
        }
    }

    private void addPostIfNotPresent(String url, boolean hasSparkRoute, Route route) {
        if (!hasSparkRoute) {
            service.post(url, route);
        }
    }

    private void addPutIfNotPresent(String url, boolean hasSparkRoute, Route route) {
        if (!hasSparkRoute) {
            service.put(url, route);
        }
    }

    private void addGetIfNotPresent(String url, boolean hasSparkRoute, Route route) {
        if (!hasSparkRoute) {
            service.get(url, route);
        }
    }

    private void addDeleteIfNotPresent(String url, boolean hasSparkRoute, Route route) {
        if (!hasSparkRoute) {
            service.delete(url, route);
        }
    }

    private Route overrideResponseByUrlWithSimple(RouteKey key, Route responseHandler) {
        if (mockResponders.get(key) == null) {
            CustomizeableDynamicRoute route = new CustomizeableDynamicRoute(responseHandler);
            mockResponders.put(key, route);
        }
        CustomizeableDynamicRoute modifiableRoute = mockResponders.get(key);
        modifiableRoute.setSimpleRouteImplementation(responseHandler);
        return modifiableRoute;
    }

    private Route overrideResponseByUrlWithStateful(RouteKey key, StatefulRoute responseHandler) {
        if (mockResponders.get(key) == null) {
            CustomizeableDynamicRoute route = new CustomizeableDynamicRoute(responseHandler, model);
            mockResponders.put(key, route);
        }
        CustomizeableDynamicRoute modifiableRoute = mockResponders.get(key);
        modifiableRoute.setRouteImplementation(responseHandler, model);
        return modifiableRoute;
    }

    private void clearRouteIfPresent(RouteKey key) {
        CustomizeableDynamicRoute customizeableDynamicRoute = mockResponders.get(key);
        if (customizeableDynamicRoute != null) {
            customizeableDynamicRoute.clearRouteImpls();
        }
    }

    private static class RouteKey {

        private HttpMethod method;

        private String url;

        RouteKey(HttpMethod method, String url) {
            this.method = method;
            this.url = url;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RouteKey that = (RouteKey) o;
            return method == that.method && Objects.equals(url, that.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, url);
        }
    }
}
