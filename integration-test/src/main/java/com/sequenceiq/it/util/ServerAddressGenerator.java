package com.sequenceiq.it.util;

public class ServerAddressGenerator {

    public static final int ADDRESS_RANGE = 254;
    private int numberOfServers;
    private String prefix;
    private int from = 1;

    public ServerAddressGenerator(int numberOfServers) {
        this.numberOfServers = numberOfServers;
        this.prefix = "192.168.";
    }

    public void iterateOver(ServerAddressGeneratorFunction serverAddressGeneratorFunction) {
        int i = from / ADDRESS_RANGE;
        int j = from % ADDRESS_RANGE;
        int serverProduceCount = from + numberOfServers - 1;
        for (; i <= serverProduceCount / ADDRESS_RANGE; i++) {
            int subAddress = Integer.min(ADDRESS_RANGE, serverProduceCount - i * ADDRESS_RANGE);
            for (; j <= subAddress; j++) {
                serverAddressGeneratorFunction.doSomething(prefix + i + "." + j);
            }
            j = 1;
        }
    }

    public void iterateOver(ServerAddressGeneratorWithNumberFunction serverAddressGeneratorWithNumberFunction) {
        int i = from / ADDRESS_RANGE;
        int j = from % ADDRESS_RANGE;
        int k = 0;
        int serverProduceCount = from + numberOfServers - 1;
        for (; i <= serverProduceCount / ADDRESS_RANGE; i++) {
            int subAddress = Integer.min(ADDRESS_RANGE, serverProduceCount - i * ADDRESS_RANGE);
            for (; j <= subAddress; j++) {
                serverAddressGeneratorWithNumberFunction.doSomething(prefix + i + "." + j, k);
                k++;
            }
            j = 1;
        }
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    @FunctionalInterface
    public interface ServerAddressGeneratorFunction {
        void doSomething(String address);
    }

    @FunctionalInterface
    public interface ServerAddressGeneratorWithNumberFunction {
        void doSomething(String address, int number);
    }
}