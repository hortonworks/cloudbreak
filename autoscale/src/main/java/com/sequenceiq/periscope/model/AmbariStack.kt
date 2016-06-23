package com.sequenceiq.periscope.model

import com.sequenceiq.periscope.domain.Ambari
import com.sequenceiq.periscope.domain.SecurityConfig

class AmbariStack @JvmOverloads constructor(val ambari: Ambari, val stackId: Long? = null, val securityConfig: SecurityConfig? = null)
