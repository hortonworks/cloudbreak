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
    default: 10.10.1.0/24
  app_net_gateway:
    type: string
    description: app network gateway address
    default: 10.10.1.1
  app_net_pool_start:
    type: string
    description: Start of app network IP address allocation pool
    default: 10.10.1.4
  app_net_pool_end:
    type: string
    description: End of app network IP address allocation pool
    default: 10.10.1.254
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
        
  ambari_instance_${agent_index}:
    type: OS::Nova::Server
    properties:
      image: { get_param: image_id }
      flavor: ${agent.flavor}
      key_name: { get_param: key_name }
      metadata: ${agent.metadata}
      networks:
        - port: { get_resource: ambari_app_port_${agent_index} }
      user_data_format: RAW
      user_data:
        str_replace:
          template: |
${userdata}
          params:
            public_net_id: { get_param: public_net_id }
   

  ambari_app_port_${agent_index}:
      type: OS::Neutron::Port
      properties:
        network_id: { get_resource: app_network }
        fixed_ips:
          - subnet_id: { get_resource: app_subnet }
        security_groups: [ { get_resource: server_security_group } ]
        
  <#list agent.volumes as volume>
  
  ambari_volume_${agent_index}_${volume_index}:
    type: OS::Cinder::Volume
    properties:
      name: hdfs-volume
      size: ${volume.size}
      volume_type: lvmdriver-1    
  
  ambari_volume_attach_${agent_index}_${volume_index}:
    type: OS::Cinder::VolumeAttachment
    properties:
      instance_uuid: { get_resource: ambari_instance_${agent_index} }
      mountpoint: ${volume.device}
      volume_id: { get_resource: ambari_volume_${agent_index}_${volume_index} }
  </#list>

  ambari_server_floatingip_${agent_index}:
    type: OS::Neutron::FloatingIP
    properties:
      floating_network_id: { get_param: public_net_id }
      port_id: { get_resource: ambari_app_port_${agent_index} }
  
  </#list>     
        

  server_security_group:
    type: OS::Neutron::SecurityGroup
    properties:
      description: Test group to demonstrate Neutron security group functionality with Heat.
      name: test-security-group
      rules: [
        {remote_ip_prefix: 0.0.0.0/0,
        protocol: tcp,
        port_range_min: 1,
        port_range_max: 65535},
        {remote_ip_prefix: 0.0.0.0/0,
        protocol: udp,
        port_range_min: 1,
        port_range_max: 65535},
        {remote_ip_prefix: 0.0.0.0/0,
        protocol: icmp}]
        
outputs:
  <#list agents as agent>
  instance_uuid_${agent_index}:
    value: { get_attr: [ambari_instance_${agent_index}, show, id] }
  </#list>