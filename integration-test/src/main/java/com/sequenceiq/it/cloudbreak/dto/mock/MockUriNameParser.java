package com.sequenceiq.it.cloudbreak.dto.mock;

import java.beans.Introspector;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class MockUriNameParser {
    private final Class clazz;

    private final Method method;

    public MockUriNameParser(Class clazz) {
        this(clazz, null);
    }

    public MockUriNameParser(Class clazz, Method method) {
        this.clazz = clazz;
        this.method = method;
    }

    public MockUriParameters getParameters() {
        String[] classNames = clazz.getName().split("\\$");
        StringBuilder sb = new StringBuilder();
        String outerClassName = "";
        for (String className : classNames) {
            Class innerClass = getInnerClass(outerClassName, className);
            String uri = parseByAnnotation(innerClass);
            if (StringUtils.isEmpty(uri)) {
                uri = parseUriByClassName(innerClass);
            }
            if (StringUtils.isNotBlank(sb) && !uri.startsWith("/")) {
                sb.append("/");
            }
            sb.append(uri);
            outerClassName = getInnerClassName(outerClassName, className);
        }
        if (StringUtils.isEmpty(sb)) {
            throw new IllegalArgumentException(
                    clazz.getName() + " Mock/uri annotation url value is empty"
            );
        }
        return new MockUriParameters(sb.toString());
    }

    private String parseByAnnotation(Class innerClass) {
        if (innerClass.isAnnotationPresent(MockUri.class)) {
            MockUri annotation = (MockUri) innerClass.getAnnotation(MockUri.class);
            return annotation.url();
        }
        return null;
    }

    private String parseUriByClassName(Class clss) {
        if (!clss.isInterface()) {
            return "";
        }
        String clssSimpleName = clss.getSimpleName();
        int indexOfBy = clssSimpleName.indexOf("By");
        StringBuilder sb = new StringBuilder();
        String path = clssSimpleName;
        String byPart = "";
        if (0 <= indexOfBy) {
            path = clssSimpleName.substring(0, indexOfBy);
            String secondPart = clssSimpleName.substring(indexOfBy + 2);
            byPart = Introspector.decapitalize(secondPart);
        }
        path = Introspector.decapitalize(path);
        if (!path.isBlank()) {
            sb.append("/").append(path);
        }
        if (StringUtils.isNotBlank(byPart)) {
            if (StringUtils.isNotBlank(sb)) {
                sb.append("/");
            }
            sb.append("{").append(byPart).append("}");
        }
        return sb.toString();
    }

    private Class getInnerClass(String outerClassName, String className) {
        String innerClassName = getInnerClassName(outerClassName, className);
        try {
            return Class.forName(innerClassName);
        } catch (ClassNotFoundException e) {
            throw new TestFailException("Cannot parse uri by class name.");
        }
    }

    private String getInnerClassName(String outerClassName, String className) {
        String innerClassName = outerClassName;
        if (!innerClassName.isEmpty()) {
            innerClassName += "$";
        }
        innerClassName += className;
        return innerClassName;
    }
}
