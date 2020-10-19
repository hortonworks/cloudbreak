package com.sequenceiq.freeipa.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieIdentityComparator;

// Sort precedence:
//   1. Cookie name (backwards - doesn't matter)
//   2. Domain name (backwards - doesn't matter)
//   3. Cookie path (more specific path is listed first) per RFC-6265 section 5.4.2
//   4. Expiration (no expiratation is listed first, latest expiration is listed next)
public class CookieComparator implements Serializable, Comparator<Cookie> {

    private CookieIdentityComparator baseRules = new CookieIdentityComparator();

    @Override
    public int compare(final Cookie lhs, final Cookie rhs) {
        // The base rules handles cookie name, domain, and path, but it has inverted logic for the more specific paths
        int res = negate(baseRules.compare(lhs, rhs));
        if (res == 0) {
            Date lhsExpiry = lhs.getExpiryDate();
            Date rhsExpiry = rhs.getExpiryDate();
            if (lhsExpiry != null && rhsExpiry == null) {
                res = 1;
            } else if (lhsExpiry == null && rhsExpiry != null) {
                res = -1;
            } else if (lhsExpiry != null && rhsExpiry != null) {
                res = lhsExpiry.compareTo(rhsExpiry);
            }
        }
        return res;
    }

    private int negate(int res) {
        if (res > 0) {
            res = -1;
        } else if (res < 0) {
            res = 1;
        }
        return res;
    }

}
