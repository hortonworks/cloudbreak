package com.sequenceiq.cloudbreak.shell.customization

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.plugin.PromptProvider
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.shell.model.ShellContext

/**
 * Manages the text of the shell's prompt.
 */
@Component
class CloudbreakPrompt : PromptProvider {

    @Autowired
    private val context: ShellContext? = null

    override fun getProviderName(): String {
        return CloudbreakPrompt::class.java!!.getSimpleName()
    }

    override fun getPrompt(): String {
        return context!!.prompt
    }
}
