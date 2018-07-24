package com.sequenceiq.cloudbreak.util.comparator;

import java.io.Serializable;
import java.util.Comparator;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;

public class AuditEventComparator implements Comparator<AuditEvent>, Serializable {

    @Override
    public int compare(AuditEvent o1, AuditEvent o2) {
        return Long.compare(o1.getOperation().getTimestamp(), o2.getOperation().getTimestamp());
    }
}
