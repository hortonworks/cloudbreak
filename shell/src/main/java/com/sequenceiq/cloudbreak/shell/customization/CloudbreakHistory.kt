package com.sequenceiq.cloudbreak.shell.customization

import org.springframework.shell.plugin.HistoryFileNameProvider
import org.springframework.stereotype.Component

/**
 * Specifies the name of the CloudBreak command log. Later this log can be used
 * to re-execute the commands with either the --cmdfile option at startup
 * or with the script --file command.
 */
@Component
class CloudbreakHistory : HistoryFileNameProvider {

    override fun getHistoryFileName(): String {
        return "cloud-break.cbh"
    }

    override fun getProviderName(): String {
        return "CloudbreakShell"
    }
}
