package com.sequenceiq.cloudbreak.shell.commands.common;

import java.util.HashMap;
import java.util.Map;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class SmartSenseSubscriptionCommands implements CommandMarker {

    private ShellContext shellContext;

    public SmartSenseSubscriptionCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator("smartsense register")
    public boolean createAvailable() {
        return true;
    }

    @CliCommand(value = "smartsense register", help = "Register the SmartSense subscription")
    public String register(@CliOption(key = "subscriptionId", mandatory = true) String subscriptionId) {
        SmartSenseSubscriptionJson subscriptionJson = new SmartSenseSubscriptionJson();
        subscriptionJson.setSubscriptionId(subscriptionId);
        try {
            Long id = shellContext.cloudbreakClient().smartSenseSubscriptionEndpoint().postPrivate(subscriptionJson).getId();
            shellContext.resetSmartSenseSubscription();
            return String.format("SmartSense subscription registered with id: '%d'", id);
        } catch (RuntimeException e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliAvailabilityIndicator("smartsense describe")
    public boolean describeAvailable() {
        return true;
    }

    @CliCommand(value = "smartsense describe", help = "Describes the SmartSense subscription")
    public String describe() {
        try {
            SmartSenseSubscriptionJson smartSenseSubscription = shellContext.cloudbreakClient().smartSenseSubscriptionEndpoint().get();
            Map<String, String> map = new HashMap<>();
            map.put("id", smartSenseSubscription.getId().toString());
            map.put("subscriptionId", smartSenseSubscription.getSubscriptionId());
            return shellContext.outputTransformer().render(OutPutType.RAW, map, "FIELD", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator({"smartsense delete --id", "smartsense delete --subscriptionId"})
    public boolean deleteAvailable() {
        return true;
    }

    private String delete(Long id, String subscriptionId) {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().smartSenseSubscriptionEndpoint().delete(id);
                shellContext.resetSmartSenseSubscription();
                return String.format("SmartSense subscription deleted with id: %s", id);
            } else if (subscriptionId != null) {
                shellContext.cloudbreakClient().smartSenseSubscriptionEndpoint().deletePrivate(subscriptionId);
                shellContext.resetSmartSenseSubscription();
                return String.format("SmartSense subscription deleted with subscription id: %s", subscriptionId);
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("Id or subscription id not specified");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "smartsense delete --id", help = "Deletes the SmartSense subscription by its id")
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) {
        return delete(id, null);
    }

    @CliCommand(value = "smartsense delete --subscriptionId", help = "Deletes the SmartSense subscription by its subscription id")
    public String deleteBySubscriptionId(@CliOption(key = "", mandatory = true) String subscriptionId) {
        return delete(null, subscriptionId);
    }
}
