package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;

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
            log(initLogMessage);
            log(String.join(", ", Arrays.stream(args).map(o -> o == null ? "null" : o.toString()).collect(Collectors.toList())));
        }
    }
}
