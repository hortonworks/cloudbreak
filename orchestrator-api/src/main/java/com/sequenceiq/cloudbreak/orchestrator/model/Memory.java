package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Objects;

public class Memory {

    private static final long KILO_BYTE = 1024;

    private static final long MEGA_BYTE = (long) Math.pow(KILO_BYTE, 2);

    private static final long GIGA_BYTE = (long) Math.pow(KILO_BYTE, 3);

    private static final long TERA_BYTE = (long) Math.pow(KILO_BYTE, 4);

    private final long valueInBytes;

    public Memory(long valueInBytes) {
        this.valueInBytes = valueInBytes;
    }

    public static Memory of(int value, String unit) {
        if ("kb".equalsIgnoreCase(unit)) {
            return new Memory(value * KILO_BYTE);
        } else if ("mb".equalsIgnoreCase(unit)) {
            return new Memory(value * MEGA_BYTE);
        } else if ("gb".equalsIgnoreCase(unit)) {
            return new Memory(value * GIGA_BYTE);
        } else if ("tb".equalsIgnoreCase(unit)) {
            return new Memory(value * TERA_BYTE);
        }
        throw new IllegalArgumentException("Unsupported unit '" + unit + "'");
    }

    public static Memory ofGigaBytes(int value) {
        return of(value, "gb");
    }

    public double getValueInGigaBytes() {
        return (double) valueInBytes / GIGA_BYTE;
    }

    public long getValueInBytes() {
        return valueInBytes;
    }

    @Override
    public String toString() {
        return "Memory{" +
                "valueInBytes=" + valueInBytes +
                ", valueGB=" + String.format("%.2f", getValueInGigaBytes()) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Memory memory = (Memory) o;
        return valueInBytes == memory.valueInBytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueInBytes);
    }
}
