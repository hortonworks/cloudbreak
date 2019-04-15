package com.sequenceiq.it.cloudbreak.newway;

import java.lang.reflect.Proxy;

import javax.annotation.Nonnull;

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
