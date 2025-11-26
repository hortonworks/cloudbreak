# ${comment}
<#if type == "SETUP" >
cat > /etc/krb5.conf.d/${filename} <<EOF
[realms]
${ipaDomain?c_upper_case} = {
  kdc = ${ipaLbFqdn}
  admin_server = ${ipaLbFqdn}
}

[domain_realm]
.${ipaDomain} = ${ipaDomain?c_upper_case}
${ipaDomain} = ${ipaDomain?c_upper_case}

[capaths]
${adDomain?c_upper_case} = {
  ${ipaDomain?c_upper_case} = ${adDomain?c_upper_case}
}

${ipaDomain?c_upper_case} = {
  ${adDomain?c_upper_case} = ${adDomain?c_upper_case}
}
EOF
chmod 644 /etc/krb5.conf.d/${filename}
<#else >
rm /etc/krb5.conf.d/${filename}
</#if>
