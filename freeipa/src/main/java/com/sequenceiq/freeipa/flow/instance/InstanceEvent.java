package com.sequenceiq.freeipa.flow.instance;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;

import reactor.rx.Promise;

public class InstanceEvent implements Selectable, Acceptable {
    private final String selector;

    private final Long resourceId;

    private final Promise<AcceptResult> accepted;

    private final List<String> instanceIds;

    public InstanceEvent(Long id) {
        this(null, id, null);
    }

    public InstanceEvent(String selector, Long resourceId, List<String> instanceIds) {
        this.selector = selector;
        this.resourceId = resourceId;
        this.instanceIds = instanceIds;
        accepted = new Promise<>();
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    @Override
    public String selector() {
        return StringUtils.isNotEmpty(selector) ? selector : EventSelectorUtil.selector(getClass());
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }
}
