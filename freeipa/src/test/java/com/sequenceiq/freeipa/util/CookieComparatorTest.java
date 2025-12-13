package com.sequenceiq.freeipa.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CookieComparatorTest {

    private CookieComparator underTest;

    @BeforeEach
    public void before() {
        underTest = new CookieComparator();
    }

    @Test
    void testCookieName() {
        Cookie c1 = mock(Cookie.class);
        Cookie c2 = mock(Cookie.class);

        when(c1.getName()).thenReturn("name1");
        when(c2.getName()).thenReturn("name2");

        assertTrue(underTest.compare(c1, c2) > 0);
        assertTrue(underTest.compare(c2, c1) < 0);
    }

    @Test
    void testCookieDomain() {
        Cookie c1 = mock(Cookie.class);
        Cookie c2 = mock(Cookie.class);

        for (Cookie c : List.of(c1, c2)) {
            when(c.getName()).thenReturn("name");
        }
        when(c1.getDomain()).thenReturn("example1.com");
        when(c2.getDomain()).thenReturn("example2.com");

        assertTrue(underTest.compare(c1, c2) > 0);
        assertTrue(underTest.compare(c2, c1) < 0);
    }

    @Test
    void testCookiePath() {
        Cookie c1 = mock(Cookie.class);
        Cookie c2 = mock(Cookie.class);
        Cookie c3 = mock(Cookie.class);

        for (Cookie c : List.of(c1, c2, c3)) {
            when(c.getName()).thenReturn("name");
            when(c.getDomain()).thenReturn("example.com");
        }
        when(c1.getPath()).thenReturn("/a");
        when(c2.getPath()).thenReturn("/path");
        when(c3.getPath()).thenReturn("/path/longer");

        assertTrue(underTest.compare(c1, c2) > 0);
        assertTrue(underTest.compare(c2, c1) < 0);
        assertTrue(underTest.compare(c1, c3) > 0);
        assertTrue(underTest.compare(c3, c1) < 0);
        assertTrue(underTest.compare(c2, c3) > 0);
        assertTrue(underTest.compare(c3, c2) < 0);
    }

    @Test
    void testCookieExpiration() throws Exception {
        Cookie c1 = mock(Cookie.class);
        Cookie c2 = mock(Cookie.class);
        Cookie c3 = mock(Cookie.class);
        Cookie c4 = mock(Cookie.class);
        Cookie c5 = mock(Cookie.class);
        Cookie c6 = mock(Cookie.class);

        for (Cookie c : List.of(c1, c2, c3, c4, c5, c6)) {
            when(c.getName()).thenReturn("name");
            when(c.getDomain()).thenReturn("example.com");
            when(c.getPath()).thenReturn("/path");
        }

        when(c1.getExpiryDate()).thenReturn(null);
        when(c2.getExpiryDate()).thenReturn(null);
        when(c3.getExpiryDate()).thenReturn(new SimpleDateFormat("yyyy-mm-dd").parse("2020-01-01"));
        when(c4.getExpiryDate()).thenReturn(new SimpleDateFormat("yyyy-mm-dd").parse("2020-01-02"));
        when(c5.getExpiryDate()).thenReturn(new SimpleDateFormat("yyyy-mm-dd").parse("2020-01-03"));
        when(c6.getExpiryDate()).thenReturn(new SimpleDateFormat("yyyy-mm-dd").parse("2020-01-03"));

        assertTrue(underTest.compare(c1, c2) == 0);
        assertTrue(underTest.compare(c2, c1) == 0);
        for (Cookie c : List.of(c3, c4, c5, c6)) {
            assertTrue(underTest.compare(c1, c) < 0);
            assertTrue(underTest.compare(c, c1) > 0);
            assertTrue(underTest.compare(c2, c) < 0);
            assertTrue(underTest.compare(c, c2) > 0);
        }
        for (Cookie c : List.of(c4, c5, c6)) {
            assertTrue(underTest.compare(c3, c) < 0);
            assertTrue(underTest.compare(c, c3) > 0);
        }
        for (Cookie c : List.of(c5, c6)) {
            assertTrue(underTest.compare(c4, c) < 0);
            assertTrue(underTest.compare(c, c4) > 0);
        }
        assertTrue(underTest.compare(c5, c6) == 0);
        assertTrue(underTest.compare(c6, c5) == 0);
    }
}