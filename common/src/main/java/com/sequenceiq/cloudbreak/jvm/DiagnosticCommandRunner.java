package com.sequenceiq.cloudbreak.jvm;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.springframework.stereotype.Component;

@Component
public class DiagnosticCommandRunner {

    public String vmFlags() throws ReflectionException, MalformedObjectNameException, InstanceNotFoundException, MBeanException {
        return run("vmFlags", "-all");
    }

    public String vmNativeMemory() throws ReflectionException, MalformedObjectNameException, InstanceNotFoundException, MBeanException {
        return run("vmNativeMemory", "summary");
    }

    private String run(String operationName, String param) throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException, MBeanException {
        return (String) ManagementFactory.getPlatformMBeanServer().invoke(new ObjectName("com.sun.management:type=DiagnosticCommand"),
                operationName,
                new Object[] { new String[] { param } },
                new String[] { String[].class.getName() });
    }
}
