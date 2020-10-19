package com.sequenceiq.freeipa.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

public class FreeIpaCookieStore implements CookieStore {

    private final Set<Cookie> cookies = new TreeSet<>(new CookieComparator());

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    @Override
    public List<Cookie> getCookies() {
        return new ArrayList<Cookie>(cookies);
    }

    @Override
    public boolean clearExpired(Date date) {
        if (date == null) {
            return false;
        }
        boolean removed = false;
        Iterator<Cookie> it = cookies.iterator();
        while (it.hasNext()) {
            if (it.next().isExpired(date)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    @Override
    public void clear() {
        cookies.clear();
    }

    @Override
    public String toString() {
        return cookies.toString();
    }
}

