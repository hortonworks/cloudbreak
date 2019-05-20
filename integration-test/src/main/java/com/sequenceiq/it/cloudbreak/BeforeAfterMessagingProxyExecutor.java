package com.sequenceiq.it.cloudbreak;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;
import com.sequenceiq.it.cloudbreak.log.Log;

public final class BeforeAfterMessagingProxyExecutor extends GenericProxyExecutor {

    @Override
    public <R> R exec(Callable<R> method) {
        try {
            return method.call();
        } catch (InvocationTargetException ite) {
            if (ite.getCause() instanceof WebApplicationException) {
                String content = ((WebApplicationException) ite.getCause()).getResponse().readEntity(String.class);
                Log.log(content);
                throw (WebApplicationException) ite.getCause();
            } else {
                throw new ProxyMethodInvocationException(getGenericExceptionMessage(), ite);
            }
        } catch (Exception e) {
            throw new ProxyMethodInvocationException(getGenericExceptionMessage(), e);
        }
    }
}
