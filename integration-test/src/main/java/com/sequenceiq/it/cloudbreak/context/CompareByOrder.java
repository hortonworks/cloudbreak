package com.sequenceiq.it.cloudbreak.context;

import java.io.Serializable;
import java.util.Comparator;

public class CompareByOrder implements Comparator<Orderable>, Serializable {

    @Override
    public int compare(Orderable o1, Orderable o2) {
        if (o1.order() > o2.order()) {
            return 1;
        } else if (o1.order() < o2.order()) {
            return -1;
        } else {
            return 0;
        }
    }
}
