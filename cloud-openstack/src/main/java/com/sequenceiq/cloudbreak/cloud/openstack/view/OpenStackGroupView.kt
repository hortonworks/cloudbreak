package com.sequenceiq.cloudbreak.cloud.openstack.view

import java.util.ArrayList

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.Group

class OpenStackGroupView(private val groups: List<Group>) {

    val flatNovaView: List<NovaInstanceView>
        get() {

            val novaInstances = ArrayList<NovaInstanceView>()
            for (group in groups) {
                for (instance in group.instances) {
                    val novaInstance = NovaInstanceView(instance.template, group.type)
                    novaInstances.add(novaInstance)
                }
            }
            return novaInstances
        }

}
