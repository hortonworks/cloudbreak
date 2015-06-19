package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class GcpConsoleOutputContext extends StackContext {

    private Compute.Instances.GetSerialPortOutput serialPortOutput;

    public GcpConsoleOutputContext(Stack stack, Compute.Instances.GetSerialPortOutput serialPortOutput) {
        super(stack);
        this.serialPortOutput = serialPortOutput;
    }

    public Compute.Instances.GetSerialPortOutput getSerialPortOutput() {
        return serialPortOutput;
    }
}