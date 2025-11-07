The following DNS Forward Zone must be added to your DNS server:

Zone: [${ipaDomain}] -> Forwarders: [<#list ipaIpAddresses as ip>${ip}<#if ip_has_next>, </#if></#list>]
