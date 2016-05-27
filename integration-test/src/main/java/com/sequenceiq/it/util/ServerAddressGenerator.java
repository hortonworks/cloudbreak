package com.sequenceiq.it.util;

public class ServerAddressGenerator {

    public static final int ADDRESS_RANGE = 254;
    private int numberOfServers;

    public ServerAddressGenerator(int numberOfServers) {
        this.numberOfServers = numberOfServers;
    }

    public void iterateOver(ServerAddressGeneratorFunction serverAddressGeneratorFunction) {
        for (int i = 0; i <= numberOfServers / ADDRESS_RANGE; i++) {
            int subAddress = Integer.min(ADDRESS_RANGE, numberOfServers - i * ADDRESS_RANGE);
            for (int j = 1; j <= subAddress; j++) {
                serverAddressGeneratorFunction.doSomething("192.168." + i + "." + j);
            }
        }
    }

    public void iterateOver(ServerAddressGeneratorWithNumberFunction serverAddressGeneratorWithNumberFunction) {
        for (int i = 0; i <= numberOfServers / ADDRESS_RANGE; i++) {
            int subAddress = Integer.min(ADDRESS_RANGE, numberOfServers - i * ADDRESS_RANGE);
            for (int j = 1; j <= subAddress; j++) {
                serverAddressGeneratorWithNumberFunction.doSomething("192.168." + i + "." + j, i * ADDRESS_RANGE + j - 1);
            }
        }
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
