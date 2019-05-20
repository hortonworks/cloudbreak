package com.sequenceiq.it.cloudbreak.finder;

public interface Finder<T> {

    T find(T element);
}
