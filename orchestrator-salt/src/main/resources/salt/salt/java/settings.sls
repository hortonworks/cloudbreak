{% set java_home = salt['environ.get']('JAVA_HOME') %}
{% if not java_home %}
  {% set java_home = '/usr/lib/jvm/java' %}
{% endif %}
{% set java_version = salt.cmd.shell("java -version 2>&1 | grep -oP \"version [^0-9]?(1\\.)?\\K\\d+\" || true") %}
{% set jre_ext_path = java_home + '/jre/lib/ext' %}


{% if java_version == "8" %}
{% set security_providerclass = 'sun.security.pkcs11.SunPKCS11' %}
{% set security_providers_template = 'salt://java/templates/java_security_providers_for_java_8.j2' %}
{% set java_policy_file_template = 'salt://java/templates/java_policy_for_java_8.policy' %}
{% set java_security_file = java_home ~ '/jre/lib/security/java.security' %}
{% set java_policy_file = java_home ~ '/jre/lib/security/java.policy' %}
{% else %}
{% set security_providerclass = 'com.safelogic.cryptocomply.jcajce.provider.CryptoComplyFipsProvider' %}
{% set security_providers_template = 'salt://java/templates/java_security_providers_for_higher_than_java_8.j2' %}
{% set java_policy_file_template = 'salt://java/templates/java_policy_for_higher_than_java_8.policy' %}
{% set java_security_file = java_home ~ '/conf/security/java.security' %}
{% set java_policy_file = java_home ~ '/conf/security/java.policy' %}
{% endif %}

{% set java = {} %}
{% do java.update({
    'java_home' : java_home,
    'java_version' : java_version,
    'jre_ext_path' : jre_ext_path,
    'security_providerclass' : security_providerclass,
    'security_providers_template' : security_providers_template,
    'java_security_file' : java_security_file,
    'java_policy_file' : java_policy_file,
    'java_policy_file_template': java_policy_file_template
}) %}