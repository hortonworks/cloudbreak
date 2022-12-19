package com.sequenceiq.freeipa.client.operation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.model.Permission;
import com.sequenceiq.freeipa.client.model.Right;

public class PermissionAddOperation extends AbstractFreeIpaAddOperation<Permission> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionAddOperation.class);

    private final List<String> right;

    private final String type;

    private final List<String> attributes;

    public PermissionAddOperation(String flag, List<Right> right, String type, List<String> attributes) {
        super(flag, Permission.class);
        this.right = right.stream().map(Right::getValue).collect(Collectors.toList());
        this.type = type;
        this.attributes = attributes;
    }

    @Override
    protected Map<String, Object> getParams() {
        return Map.of("ipapermright", right,
                "type", type,
                "attrs", attributes);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String getOperationName() {
        return "permission_add";
    }
}
