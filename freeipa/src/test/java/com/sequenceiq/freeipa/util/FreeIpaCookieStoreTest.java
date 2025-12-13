package com.sequenceiq.freeipa.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FreeIpaCookieStoreTest {

    private FreeIpaCookieStore underTest;

    private Cookie cookie1;

    private Cookie cookie2;

    private Cookie cookie3;

    private Cookie expiredCookie;

    @BeforeEach
    public void before() throws Exception {
        underTest = new FreeIpaCookieStore();
        cookie1 = createCookie("name", "example.com", "/path", new SimpleDateFormat("yyyy-mm-dd").parse("2050-01-01"));
        cookie2 = createCookie("name", "example.com", "/path", null);
        cookie3 = createCookie("name", "example.com", "/path/longer", new SimpleDateFormat("yyyy-mm-dd").parse("2050-01-01"));
        expiredCookie = createCookie("name", "example.com", "/path", new SimpleDateFormat("yyyy-mm-dd").parse("2000-01-01"));
    }

    @Test
    void testAddCookie() {
        assertEquals(underTest.getCookies().size(), 0);
        underTest.addCookie(cookie1);
        assertEquals(underTest.getCookies().size(), 1);
        assertTrue(underTest.getCookies().containsAll(List.of(cookie1)));
        underTest.addCookie(cookie2);
        assertEquals(underTest.getCookies().size(), 2);
        assertTrue(underTest.getCookies().containsAll(List.of(cookie1, cookie2)));
    }

    @Test
    void testGetCookiesOrder() {
        underTest.addCookie(cookie1);
        underTest.addCookie(cookie2);
        assertEquals(underTest.getCookies(), List.of(cookie2, cookie1));
        underTest.addCookie(cookie3);
        assertEquals(underTest.getCookies(), List.of(cookie3, cookie2, cookie1));
    }

    @Test
    void testClear() {
        underTest.addCookie(cookie1);
        assertFalse(underTest.getCookies().isEmpty());
        underTest.clear();
        assertTrue(underTest.getCookies().isEmpty());
    }

    @Test
    void testClearExpired() throws Exception {
        underTest.addCookie(cookie1);
        underTest.addCookie(cookie2);
        underTest.addCookie(expiredCookie);
        assertEquals(underTest.getCookies().size(), 3);
        underTest.clearExpired(new SimpleDateFormat("yyyy-mm-dd").parse("2020-01-01"));
        assertEquals(underTest.getCookies().size(), 2);
        assertTrue(underTest.getCookies().containsAll(List.of(cookie1, cookie2)));
        assertFalse(underTest.getCookies().contains(expiredCookie));
    }

    private Cookie createCookie(String name, String domain, String path, Date expiry) {
        return new Cookie() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getValue() {
                return null;
            }

            @Override
            public String getComment() {
                return null;
            }

            @Override
            public String getCommentURL() {
                return null;
            }

            @Override
            public Date getExpiryDate() {
                return expiry;
            }

            @Override
            public boolean isPersistent() {
                return false;
            }

            @Override
            public String getDomain() {
                return domain;
            }

            @Override
            public String getPath() {
                return path;
            }

            @Override
            public int[] getPorts() {
                return new int[0];
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public int getVersion() {
                return 0;
            }

            @Override
            public boolean isExpired(Date date) {
                return expiry != null && date.after(expiry);
            }
        };
    }
}