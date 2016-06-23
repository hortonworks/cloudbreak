package com.sequenceiq.cloudbreak.shell.customization

import org.springframework.shell.plugin.BannerProvider
import org.springframework.shell.support.util.FileUtils
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.shell.CloudbreakShell

/**
 * Prints the banner when the user starts the shell.
 */
@Component
class CloudbreakBanner : BannerProvider {

    override fun getProviderName(): String {
        return "CloudbreakShell"
    }

    override fun getBanner(): String {
        return FileUtils.readBanner(CloudbreakShell::class.java, "banner.txt")
    }

    override fun getVersion(): String {
        return javaClass.getPackage().getImplementationVersion()
    }

    override fun getWelcomeMessage(): String {
        return "Welcome to Cloudbreak Shell. For command and param completion press TAB, for assistance type 'hint'."
    }
}
