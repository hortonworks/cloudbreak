package com.sequenceiq.cloudbreak.service.stack.flow

import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.StackContext

import net.schmizz.sshj.transport.verification.HostKeyVerifier

class SshCheckerTaskContext(stack: Stack, val hostKeyVerifier: HostKeyVerifier, val publicIp: String, val sshPort: Int, val user: String, val sshPrivateFileLocation: String) : StackContext(stack)
