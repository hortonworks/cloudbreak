package com.sequenceiq.it.cloudbreak.dto.mock;

import org.springframework.http.HttpMethod;

public class Method {
    private HttpMethod httpMethod;

    private String methodName;

    private Method(HttpMethod httpMethod, String methodName) {
        this.httpMethod = httpMethod;
        this.methodName = methodName;
    }

    public static Method build(String methodName) {
        HttpMethod httpMethod = null;
        try {
            httpMethod = HttpMethod.valueOf(methodName.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (HttpMethod checkMethod : HttpMethod.values()) {
                if (methodName.startsWith(checkMethod.name().toLowerCase())) {
                    httpMethod = checkMethod;
                    break;
                }
            }
        }
        if (httpMethod == null) {
            throw new IllegalArgumentException(methodName + " method name should start with http method (get, post, ...)");
        }
        return new Method(httpMethod, methodName);
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Method) {
            Method aMethod = (Method) obj;
            return aMethod.getMethodName().equals(this.getMethodName());
        }
        return false;
    }

    @Override
    public String toString() {
        return getMethodName();
    }

    @Override
    public int hashCode() {
        return getMethodName().hashCode();
    }
}
