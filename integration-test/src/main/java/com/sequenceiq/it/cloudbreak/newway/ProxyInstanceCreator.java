package com.sequenceiq.it.cloudbreak.newway;

import javax.annotation.Nonnull;
import java.lang.reflect.Proxy;

final class ProxyInstanceCreator {

    private final ProxyHandler<?> handler;

    ProxyInstanceCreator(@Nonnull ProxyHandler<?> handler) {
        this.handler = handler;
    }

    @SuppressWarnings("unchecked")
    <I> I createProxy(Class<I> clazz) {
        return (I) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
    }

}
