package com.sequenceiq.it.cloudbreak.dto.mock;

import java.lang.reflect.Method;

import org.springframework.util.StringUtils;

class SparkUriAnnotationHandler {
    private final Class clazz;

    private final Method method;

    SparkUriAnnotationHandler(Class clazz, Method method) {
        this.clazz = clazz;
        this.method = method;
    }

    public SparkUriParameters getParameters() {
        SparkUri annotation;
        if (method.isAnnotationPresent(SparkUri.class)) {
            annotation = method.getAnnotation(SparkUri.class);

        } else if (clazz.isAnnotationPresent(SparkUri.class)) {
            annotation = (SparkUri) clazz.getAnnotation(SparkUri.class);
        } else {
            throw new IllegalArgumentException(
                    clazz.getName() + "or " + method.getName() + " does not have SparkUri annotation"
            );
        }
        String url = annotation.url();
        Class type = annotation.requestType();
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException(
                    clazz.getName() + "or " + method.getName() + " SparkUri annotation url value is empty"
            );
        }
        return new SparkUriParameters(url, type);
    }
}
