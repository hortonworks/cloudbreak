package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;

class DatabaseUtilTest {

    private static final String INITIALIZED_TO_STRING = "initialized";

    private static final String SUPPLIED_TO_STRING = "supplied toString()";

    private static final String NOT_PROXY_OBJECT = "not proxy object";

    @Test
    void isLazyLoadInitializedFalseWhenProxyIsUninitialized() {
        Object proxyObject = createProxyObject(false);

        boolean result = DatabaseUtil.isLazyLoadInitialized(proxyObject);

        assertThat(result).isFalse();
    }

    @Test
    void isLazyLoadInitializedTrueWhenProxyIsInitialized() {
        Object proxyObject = createProxyObject(true);

        boolean result = DatabaseUtil.isLazyLoadInitialized(proxyObject);

        assertThat(result).isTrue();
    }

    @Test
    void isLazyLoadInitializedTrueWhenObjectIsNotProxy() {
        boolean result = DatabaseUtil.isLazyLoadInitialized(NOT_PROXY_OBJECT);

        assertThat(result).isTrue();
    }

    @Test
    void lazyLoadSafeToStringWhenProxyIsUninitialized() {
        Object proxyObject = createProxyObject(false);

        String result = DatabaseUtil.lazyLoadSafeToString(proxyObject);

        assertThat(result).isEqualTo(DatabaseUtil.UNINITIALIZED_TO_STRING);
    }

    @Test
    void lazyLoadSafeToStringWhenProxyIsInitialized() {
        Object proxyObject = createProxyObject(true);

        String result = DatabaseUtil.lazyLoadSafeToString(proxyObject);

        assertThat(result).isEqualTo(INITIALIZED_TO_STRING);
    }

    @Test
    void lazyLoadSafeToStringWhenObjectIsNotProxy() {
        String result = DatabaseUtil.lazyLoadSafeToString(NOT_PROXY_OBJECT);

        assertThat(result).isEqualTo((Object) NOT_PROXY_OBJECT);
    }

    @Test
    void lazyLoadSafeToStringWithSupplierWhenProxyIsUninitialized() {
        Object proxyObject = createProxyObject(false);

        String result = DatabaseUtil.lazyLoadSafeToString(proxyObject, o -> SUPPLIED_TO_STRING);

        assertThat(result).isEqualTo(DatabaseUtil.UNINITIALIZED_TO_STRING);
    }

    @Test
    void lazyLoadSafeToStringWithSupplierWhenProxyIsInitialized() {
        Object proxyObject = createProxyObject(true);

        String result = DatabaseUtil.lazyLoadSafeToString(proxyObject, o -> SUPPLIED_TO_STRING);

        assertThat(result).isEqualTo(SUPPLIED_TO_STRING);
    }

    @Test
    void lazyLoadSafeToStringWithSupplierWhenObjectIsNotProxy() {
        String result = DatabaseUtil.lazyLoadSafeToString(NOT_PROXY_OBJECT, o -> SUPPLIED_TO_STRING);

        assertThat(result).isEqualTo(SUPPLIED_TO_STRING);
    }

    /**
     * @param initialized the expected return value of {@link org.hibernate.Hibernate#isInitialized(Object)} when called on the result {@link Object}
     */
    private Object createProxyObject(boolean initialized) {
        LazyInitializer lazyInitializer = mock(LazyInitializer.class);
        when(lazyInitializer.isUninitialized()).thenReturn(!initialized);
        HibernateProxy hibernateProxy = mock(HibernateProxy.class);
        when(hibernateProxy.getHibernateLazyInitializer()).thenReturn(lazyInitializer);
        when(hibernateProxy.toString()).thenReturn(INITIALIZED_TO_STRING);
        return hibernateProxy;
    }

}
