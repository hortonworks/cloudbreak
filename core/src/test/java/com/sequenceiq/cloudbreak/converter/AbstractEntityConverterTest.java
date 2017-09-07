package com.sequenceiq.cloudbreak.converter;

public abstract class AbstractEntityConverterTest<S> extends AbstractConverterTest {

    private final S source = createSource();

    public abstract S createSource();

    public S getSource() {
        return source;
    }
}
