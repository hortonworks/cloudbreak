package com.sequenceiq.cloudbreak.cloud.polling;

public class NumericPollingReference implements PollingReference<Long> {
    private Long referenceData;

    public NumericPollingReference(Long referenceData) {
        this.referenceData = referenceData;
    }

    @Override
    public Long referenceData() {
        return referenceData;
    }

    //BEGIN GENERATED CODE

    @Override
    public String toString() {
        return "NumericPollingReference{" +
                "referenceData=" + referenceData +
                '}';
    }
    //END GENERATED CODE

}
