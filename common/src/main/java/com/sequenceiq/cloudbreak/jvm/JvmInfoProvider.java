package com.sequenceiq.cloudbreak.jvm;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JvmInfoProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JvmInfoProvider.class);

    @Value("${vm.info.vmflags.enabled:true}")
    private boolean vmFlagsEnabled;

    @Inject
    private VmFlagsParser vmFlagsParser;

    @Inject
    private DiagnosticCommandRunner diagnosticCommandRunner;

    @PostConstruct
    public void init() {
        if (vmFlagsEnabled) {
            printVmFlags();
        }
    }

    private void printVmFlags() {
        try {
            String vmFlags = diagnosticCommandRunner.vmFlags();
            List<String> vmFlagList = vmFlagsParser.parseVmFlags(vmFlags);
            LOGGER.debug("VM flags: {}", vmFlagList);
        } catch (Exception e) {
            LOGGER.error("Cannot print VM flags", e);
        }
    }
}
