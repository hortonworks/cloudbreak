package com.sequenceiq.it.cloudbreak.finder;

import java.util.Collection;

public class Finders {

    private Finders() {

    }

    public static <T> Finder<T> first() {
        return list -> ((Collection<T>) list).stream().findFirst().orElse(null);
    }

    public static <T> Finder<T> same() {
        return element -> element;
    }
}
