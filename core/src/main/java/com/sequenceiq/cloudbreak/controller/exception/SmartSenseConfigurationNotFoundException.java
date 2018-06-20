package com.sequenceiq.cloudbreak.controller.exception;

public class SmartSenseConfigurationNotFoundException extends RuntimeException {

    public SmartSenseConfigurationNotFoundException() {
        super("SmartSense configuration not found");
    }
}
