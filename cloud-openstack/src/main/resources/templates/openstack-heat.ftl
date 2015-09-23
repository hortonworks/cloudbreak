<#setting number_format="computer">
heat_template_version: 2014-10-16

description: >
  Heat template for Cloudbreak

parameters:

  key_name:
    type: string
    description : Name of a KeyPair to enable SSH access to the instance
  image_id:
    type: string
    description: ID of the image
  app_net_cidr:
    type: string
    description: app network address (CIDR notation)
  public_net_id:
    type: string
    description: The ID of the public network. You will need to replace it with your DevStack public network ID

resources:

  app_network:
      type: OS::Neutron::Net
      properties:
        admin_state_up: true
        name: app_network

  app_subnet:
      type: OS::Neutron::Subnet
      properties:
        network_id: { get_resource: app_network }
        cidr: { get_param: app_net_cidr } 

  router:
      type: OS::Neutron::Router

  router_gateway:
      type: OS::Neutron::RouterGateway
      properties:
        router_id: { get_resource: router }
        network_id: { get_param: public_net_id }

  router_interface:
      type: OS::Neutron::RouterInterface
      properties:
        router_id: { get_resource: router }
        subnet_id: { get_resource: app_subnet }
        
  gw_user_data_config:
      type: OS::Heat::SoftwareConfig
      properties:
        config: |
${gateway_user_data}

  core_user_data_config:
      type: OS::Heat::SoftwareConfig
      properties:
        config: |
${core_user_data}

  <#list agents as agent>

  ambari_${agent.instanceId}:
    type: OS::Nova::Server
    properties:
      image: { get_param: image_id }
      flavor: ${agent.flavor}
      key_name: { get_param: key_name }
      admin_user: centos
      metadata: ${agent.metadata}
      networks:
        - port: { get_resource: ambari_app_port_${agent.instanceId} }
      block_device_mapping:
      <#list agent.volumes as volume>
      - device_name: ${volume.device}
        volume_id: { get_resource: ambari_volume_${agent.instanceId}_${volume_index} }
      </#list>

      user_data_format: SOFTWARE_CONFIG
      <#if agent.type == "GATEWAY">
      user_data:  { get_resource: gw_user_data_config }
      <#elseif agent.type == "CORE">
      user_data:  { get_resource: core_user_data_config }
      </#if>

  ambari_app_port_${agent.instanceId}:
      type: OS::Neutron::Port
      properties:
        network_id: { get_resource: app_network }
        replacement_policy: AUTO
        fixed_ips:
          - subnet_id: { get_resource: app_subnet }
        security_groups: [ { get_resource: server_security_group } ]
        
<#list agent.volumes as volume>

  ambari_volume_${agent.instanceId}_${volume_index}:
    type: OS::Cinder::Volume
    properties:
      name: hdfs-volume
      size: ${volume.size}

</#list>

  ambari_server_floatingip_${agent.instanceId}:
    type: OS::Neutron::FloatingIP
    properties:
      floating_network_id: { get_param: public_net_id }
      port_id: { get_resource: ambari_app_port_${agent.instanceId} }
  
  </#list>     

  server_security_group:
    type: OS::Neutron::SecurityGroup
    properties:
      description: Cloudbreak security group
      name: cb-sec-group_${cb_stack_name}
      rules: [
        <#list rules as r>
        <#list r.getPorts() as p>
        {remote_ip_prefix: ${r.cidr},
        protocol: ${r.protocol},
        port_range_min: ${p},
        port_range_max: ${p}},
        </#list>
        </#list>
        {remote_ip_prefix: { get_param: app_net_cidr },
        protocol: tcp,
        port_range_min: 1,
        port_range_max: 65535},
        {remote_ip_prefix: { get_param: app_net_cidr },
        protocol: udp,
        port_range_min: 1,
        port_range_max: 65535},
        {remote_ip_prefix: 0.0.0.0/0,
        protocol: icmp}]

        
outputs:
  <#list agents as agent>
  instance_uuid_${agent.instanceId}:
    value: { get_attr: [ambari_${agent.instanceId}, show, id] }
  </#list>