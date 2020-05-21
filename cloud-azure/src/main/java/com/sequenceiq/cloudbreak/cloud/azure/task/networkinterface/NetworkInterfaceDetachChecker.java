package com.sequenceiq.cloudbreak.cloud.azure.task.networkinterface;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.network.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(NetworkInterfaceDetachChecker.NAME)
@Scope("prototype")
public class NetworkInterfaceDetachChecker extends PollBooleanStateTask {

    public static final String NAME = "NetworkInterfaceDetachChecker";

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkInterfaceDetachChecker.class);

    private NetworkInterfaceDetachCheckerContext context;

    private Collection<String> attachedNetworkInterfaces;

    public NetworkInterfaceDetachChecker(AuthenticatedContext authenticatedContext, NetworkInterfaceDetachCheckerContext context) {
        super(authenticatedContext, false);
        this.context = context;
        this.attachedNetworkInterfaces = context.getNetworkInterfaceNames();
    }

    @Override
    protected Boolean doCall() {
        LOGGER.info("Waiting for network interfaces to be detached: {}", attachedNetworkInterfaces);

        AzureClient azureClient = context.getAzureClient();
        String resourceGroupName = context.getResourceGroupName();
        List<NetworkInterface> filteredNetworkInterfaces = azureClient.getNetworkInterfaceListByNames(resourceGroupName, attachedNetworkInterfaces);

        attachedNetworkInterfaces = filteredNetworkInterfaces
                .stream()
                .filter(ni -> ni.virtualMachineId() != null)
                .map(NetworkInterface::name)
                .collect(Collectors.toSet());
        return attachedNetworkInterfaces.isEmpty();
    }
}
