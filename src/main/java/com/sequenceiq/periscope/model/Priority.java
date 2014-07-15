package com.sequenceiq.periscope.model;

import java.util.Comparator;

public class Priority {

    public static final Priority HIGHEST = Priority.of(0);
    public static final Priority NORMAL = Priority.of(100);
    public static final Priority LOWEST = Priority.of(Integer.MAX_VALUE);

    private final int value;

    private Priority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Priority priority = (Priority) o;

        if (value != priority.value) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return value;
    }

    public static Priority of(int value) {
        int val = value < 0 ? 0 : value;
        val = val > Integer.MAX_VALUE ? Integer.MAX_VALUE : val;
        return new Priority(val);
    }

    public static class PriorityComparator implements Comparator<Priority> {
        @Override
        public int compare(Priority o1, Priority o2) {
            return Integer.compare(o1.value, o2.value);
        }
    }

}
