package com.sequenceiq.cloudbreak.shell.customization;

import org.springframework.shell.plugin.BannerProvider;
import org.springframework.shell.support.util.FileUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.shell.CloudbreakShell;

/**
 * Prints the banner when the user starts the shell.
 */
@Component
public class CloudbreakBanner implements BannerProvider {

    @Override
    public String getProviderName() {
        return "CloudbreakShell";
    }

    @Override
    public String getBanner() {
        return FileUtils.readBanner(CloudbreakShell.class, "banner.txt");
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getWelcomeMessage() {
        return "Welcome to Cloudbreak Shell. For command and param completion press TAB, for assistance type 'hint'.";
    }
}
