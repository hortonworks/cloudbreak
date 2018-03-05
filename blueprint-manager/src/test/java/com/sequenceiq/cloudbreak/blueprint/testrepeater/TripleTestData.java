package com.sequenceiq.cloudbreak.blueprint.testrepeater;

public abstract class TripleTestData<A, B, C> {

    private final A data1;

    private final B data2;

    private final C data3;

    protected TripleTestData(A data1, B data2, C data3) {
        this.data1 = data1;
        this.data2 = data2;
        this.data3 = data3;
    }

    protected A getData1() {
        return data1;
    }

    protected B getData2() {
        return data2;
    }

    protected C getData3() {
        return data3;
    }

}
