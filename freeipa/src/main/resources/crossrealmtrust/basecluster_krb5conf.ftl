# Extend krb5.conf with the following content
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
