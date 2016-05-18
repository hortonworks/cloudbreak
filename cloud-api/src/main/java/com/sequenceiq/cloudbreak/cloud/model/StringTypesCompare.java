package com.sequenceiq.cloudbreak.cloud.model;

import static java.lang.Character.isDigit;
import static java.lang.Character.isSpaceChar;

import java.util.Comparator;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class StringTypesCompare implements Comparator<StringType> {

    int compareRight(String a, String b) {
        int bias = 0;
        int ia = 0;
        int ib = 0;
        for (;; ia++, ib++) {
            char ca = charAt(a, ia);
            char cb = charAt(b, ib);
            if (!isDigit(ca) && !isDigit(cb)) {
                return bias;
            } else if (!isDigit(ca)) {
                return -1;
            } else if (!isDigit(cb)) {
                return +1;
            } else if (ca < cb) {
                if (bias == 0) {
                    bias = -1;
                }
            } else if (ca > cb) {
                if (bias == 0) {
                    bias = +1;
                }
            } else if (ca == 0 && cb == 0) {
                return bias;
            }
        }
    }

    public int compare(StringType o1, StringType o2) {
        String a = o1.value().toString();
        String b = o2.value().toString();

        int ia = 0;
        int ib = 0;
        int nza = 0;
        int nzb = 0;
        char ca;
        char cb;
        int result;

        while (true) {
            nza = 0;
            nzb = 0;
            ca = charAt(a, ia);
            cb = charAt(b, ib);
            while (isSpaceChar(ca) || ca == '0') {
                if (ca == '0') {
                    nza++;
                } else {
                    nza = 0;
                }
                ca = charAt(a, ++ia);
            }

            while (isSpaceChar(cb) || cb == '0') {
                if (cb == '0') {
                    nzb++;
                } else {
                    nzb = 0;
                }
                cb = charAt(b, ++ib);
            }

            if (isDigit(ca) && isDigit(cb)) {
                result = compareRight(a.substring(ia), b.substring(ib));
                if (result != 0) {
                    return result;
                }
            }
            if (ca == 0 && cb == 0) {
                return nza - nzb;
            }
            if (ca < cb) {
                return -1;
            } else if (ca > cb) {
                return +1;
            }

            ++ia;
            ++ib;
        }
    }

    char charAt(String s, int i) {
        if (i >= s.length()) {
            return 0;
        } else {
            return s.charAt(i);
        }
    }
}