heat_template_version: 2014-10-16

description: >
  Heat OpenStack-native for Ambari

parameters:

  key_name:
    type: string
    description : Name of a KeyPair to enable SSH access to the instance
  tenant_id:
    type: string
    description : ID of the tenant
  image_id:
    type: string
    description: ID of the image
    default: Ubuntu 14.04 LTS amd64
  app_net_cidr:
    type: string
    description: app network address (CIDR notation)
    default: ${cbSubnet}
  app_net_gateway:
    type: string
    description: app network gateway address
    default: ${gatewayIP}
  app_net_pool_start:
    type: string
    description: Start of app network IP address allocation pool
    default: ${startIP}
  app_net_pool_end:
    type: string
    description: End of app network IP address allocation pool
    default: ${endIP}
  public_net_id:
    type: string
    description: The ID of the public network. You will need to replace it with your DevStack public network ID

resources:

  app_network:
      type: OS::Neutron::Net
      properties:
        admin_state_up: true
        name: app_network
        shared: true
        tenant_id: { get_param: tenant_id }

  app_subnet:
      type: OS::Neutron::Subnet
      properties:
        network_id: { get_resource: app_network }
        cidr: { get_param: app_net_cidr } 
        gateway_ip: { get_param: app_net_gateway }
        allocation_pools:
          - start: { get_param: app_net_pool_start }
            end: { get_param: app_net_pool_end }

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
        
  <#list agents as agent>
  <#assign metadata = agent.metadata?eval>
  <#assign instance_id = metadata.cb_instance_group_name?replace('_', '') + "_" + metadata.cb_instance_private_id>
  <#if agent.type == "GATEWAY">
     <#assign userdata = gatewayuserdata>
  <#elseif agent.type == "HOSTGROUP">
     <#assign userdata = hostgroupuserdata>
  </#if>

  ambari_${instance_id}:
    type: OS::Nova::Server
    properties:
      image: { get_param: image_id }
      flavor: ${agent.flavor}
      key_name: { get_param: key_name }
      metadata: ${agent.metadata}
      networks:
        - port: { get_resource: ambari_app_port_${instance_id} }
      user_data_format: RAW
      user_data:
        str_replace:
          template: |
${userdata}
          params:
            public_net_id: { get_param: public_net_id }

  ambari_app_port_${instance_id}:
      type: OS::Neutron::Port
      properties:
        network_id: { get_resource: app_network }
        replacement_policy: AUTO
        fixed_ips:
          - subnet_id: { get_resource: app_subnet }
        security_groups: [ { get_resource: server_security_group } ]
        
  <#list agent.volumes as volume>
  
  ambari_volume_${instance_id}_${volume_index}:
    type: OS::Cinder::Volume
    properties:
      name: hdfs-volume
      size: ${volume.size}
      volume_type: lvmdriver-1    
  
  ambari_volume_attach_${instance_id}_${volume_index}:
    type: OS::Cinder::VolumeAttachment
    properties:
      instance_uuid: { get_resource: ambari_${instance_id} }
      mountpoint: ${volume.device}
      volume_id: { get_resource: ambari_volume_${instance_id}_${volume_index} }
  </#list>

  ambari_server_floatingip_${instance_id}:
    type: OS::Neutron::FloatingIP
    properties:
      floating_network_id: { get_param: public_net_id }
      port_id: { get_resource: ambari_app_port_${instance_id} }
  
  </#list>     

  server_security_group:
    type: OS::Neutron::SecurityGroup
    properties:
      description: Cloudbreak security group
      name: cb-sec-group
      rules: [
        <#list subnets as s>
        <#list ports as p>
        {remote_ip_prefix: ${s.cidr},
        protocol: ${p.protocol},
        port_range_min: ${p.localPort},
        port_range_max: ${p.localPort}},
        </#list>
        </#list>
        {remote_ip_prefix: ${cbSubnet},
        protocol: tcp,
        port_range_min: 1,
        port_range_max: 65535},
        {remote_ip_prefix: ${cbSubnet},
        protocol: udp,
        port_range_min: 1,
        port_range_max: 65535},
        {remote_ip_prefix: ${cbSubnet},
        protocol: icmp}]
        
outputs:
  <#list agents as agent>
  <#assign m = agent.metadata?eval>
  <#assign instance_id = m.cb_instance_group_name?replace('_', '') + "_" + m.cb_instance_private_id>
  instance_uuid_${instance_id}:
    value: { get_attr: [ambari_${instance_id}, show, id] }
  </#list>