package com.sequenceiq.cloudbreak.comparator.audit;

import java.io.Serializable;
import java.util.Comparator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;

public class AuditEventComparator implements Comparator<AuditEventV4Response>, Serializable {

    @Override
    public int compare(AuditEventV4Response o1, AuditEventV4Response o2) {
        return Long.compare(o1.getAuditId(), o2.getAuditId());
    }
}
