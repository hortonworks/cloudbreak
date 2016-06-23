package com.sequenceiq.cloudbreak.domain

import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator

import com.sequenceiq.cloudbreak.common.type.ComponentType
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.domain.json.JsonToString

@Entity
@NamedQueries(@NamedQuery(
        name = "Component.findComponentByStackIdComponentTypeName",
        query = "SELECT cv FROM Component cv "
                + "WHERE cv.stack.id = :stackId AND cv.componentType = :componentType AND cv.name = :name"))
class Component {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "component_generator")
    @SequenceGenerator(name = "component_generator", sequenceName = "component_id_seq", allocationSize = 1)
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    var componentType: ComponentType? = null

    var name: String? = null

    @Convert(converter = JsonToString::class)
    @Column(columnDefinition = "TEXT")
    var attributes: Json? = null

    @ManyToOne
    var stack: Stack? = null


    constructor() {

    }

    constructor(componentType: ComponentType, name: String, attributes: Json, stack: Stack) {
        this.componentType = componentType
        this.name = name
        this.attributes = attributes
        this.stack = stack
    }

    override fun toString(): String {
        return "Component{"
        +"id=" + id
        +", componentType=" + componentType
        +", name='" + name + '\''
        +'}'
    }
}
