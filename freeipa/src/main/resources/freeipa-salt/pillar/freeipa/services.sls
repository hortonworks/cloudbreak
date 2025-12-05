freeipa:
  services:
    - gssproxy
    - httpd
    - kadmin
    - krb5kdc
{%- if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int > 8 %}
    - named
{%- else %}
    - named-pkcs11
    - polkit
{%- endif %}
    - pki-tomcatd@pki-tomcat
    - certmonger
    - sssd
    - nginx
