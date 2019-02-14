package com.sequenceiq.cloudbreak.clusterdefinition.testrepeater;

import java.util.Objects;

public class Tuple<A, B> {

    private final A input;

    private final B output;

    public Tuple(A anA, B aB) {
        input = anA;
        output = aB;
    }

    public A input() {
        return input;
    }

    public B output() {
        return output;
    }

    @Override
    public String toString() {
        return "<[" + input + ", " + output + "]>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        Tuple<?, ?> tuple = (Tuple<?, ?>) o;
        return Objects.equals(input, tuple.input)
                && Objects.equals(output, tuple.output);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, output);
    }
}