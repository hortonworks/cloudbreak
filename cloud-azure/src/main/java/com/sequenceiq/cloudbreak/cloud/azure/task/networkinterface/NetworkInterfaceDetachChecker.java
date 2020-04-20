package com.sequenceiq.cloudbreak.cloud.azure.task.networkinterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.network.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

import rx.Completable;
import rx.schedulers.Schedulers;

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
        List<Completable> networkInterfacesCompletables = new ArrayList<>();
        List<NetworkInterface> networkInterfaces = new ArrayList<>();
        List<String> failedToRetriveNetworkInterfaces = new ArrayList<>();
        for (String networkInterfaceName : attachedNetworkInterfaces) {
            networkInterfacesCompletables.add(Completable.fromObservable(
                    context.getAzureClient().getNetworkInterfaceAsync(context.getResourceGroupName(), networkInterfaceName)
                            .doOnError(throwable -> {
                                LOGGER.error("Error happened on azure network interface retrieval: {}", networkInterfaceName, throwable);
                                failedToRetriveNetworkInterfaces.add(networkInterfaceName);
                            })
                            .doOnNext(ni -> networkInterfaces.add(ni))
                            .subscribeOn(Schedulers.io())));
        }
        Completable.merge(networkInterfacesCompletables).await();
        if (!failedToRetriveNetworkInterfaces.isEmpty()) {
            LOGGER.error("Can't retrieve the following network interfaces: {}", failedToRetriveNetworkInterfaces);
            throw new CloudbreakServiceException("Can't retrieve the following network interfaces: " + failedToRetriveNetworkInterfaces);
        }
        attachedNetworkInterfaces = networkInterfaces.stream()
                .filter(ni -> ni.virtualMachineId() != null).map(NetworkInterface::name).collect(Collectors.toSet());
        return attachedNetworkInterfaces.isEmpty();
    }
}
