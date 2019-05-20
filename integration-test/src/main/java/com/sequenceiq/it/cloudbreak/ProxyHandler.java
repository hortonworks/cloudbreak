package com.sequenceiq.it.cloudbreak;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ProxyHandler<I> implements InvocationHandler {

    private final I original;

    private final GenericProxyExecutor executor;

    public ProxyHandler(I original, GenericProxyExecutor executor) {
        this.original = original;
        this.executor = executor;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        try {
            method.setAccessible(true);
            return executor.exec(() -> method.invoke(original, args));
        } catch (WebApplicationException | ProxyMethodInvocationException e) {
            printArgs(method, args);
            throw e;
        }
    }

    private void printArgs(Method method, Object[] args) {
        if (args != null) {
            String[] declaringClass = method.getDeclaringClass().toString().split(" ");
            String initLogMessage = declaringClass.length == 2
                    ? String.format("Method has ['%s.%s'] called with args: ", method.getDeclaringClass().toString().split(" ")[1], method.getName())
                    : String.format("Method has ['%s'] called with args: ", method.getName());
            Log.log(initLogMessage);
            Log.log(String.join(", ", Arrays.stream(args).map(o -> o == null ? "null" : o.toString()).collect(Collectors.toList())));
        }
    }
}
