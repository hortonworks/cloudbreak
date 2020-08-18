<#setting number_format="computer">
heat_template_version: 2014-10-16

description: >
  Heat template for Environment

parameters:

  app_net_cidr:
    type: string
    description: app network address (CIDR notation)
  <#if network.assignFloatingIp>
  public_net_id:
    type: string
    description: The ID of the public network. You will need to replace it with your DevStack public network ID
  </#if>

resources:

  app_network:
      type: OS::Neutron::Net
      properties:
        admin_state_up: true
        name: app_network

  <#list subnets as subnet>

  app_subnet:
      type: OS::Neutron::Subnet
      properties:
        network_id: { get_resource: app_network }
        cidr: ${subnet.cidr}

  router:
      type: OS::Neutron::Router

  router_gateway:
      type: OS::Neutron::RouterGateway
      properties:
        router_id: { get_resource: router }
        <#if subnet.assignFloatingIp>
        network_id: { get_param: public_net_id }
        </#if>

  router_interface:
      type: OS::Neutron::RouterInterface
      properties:
        router_id: { get_resource: router }
        subnet_id: { get_resource: app_subnet }
