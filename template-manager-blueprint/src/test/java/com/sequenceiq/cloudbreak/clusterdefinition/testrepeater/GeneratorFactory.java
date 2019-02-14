package com.sequenceiq.cloudbreak.clusterdefinition.testrepeater;

import static java.util.Arrays.asList;

public final class GeneratorFactory {

    private GeneratorFactory() {
    }

    @SafeVarargs
    public static <T> Generator<T> list(T... values) {
        return new ListGenerator<>(asList(values));
    }

    public static <A, B> Generator<Tuple<A, B>> tuples(Iterable<A> listOfAs,
            Iterable<B> listOfBs) {
        return new TupleGenerator<>(listOfAs, listOfBs);
    }

}
