{% set databus = {} %}
{% if salt['pillar.get']('databus:enabled') %}
    {% set databus_enabled = True %}
{% else %}
    {% set databus_enabled = False %}
{% endif %}

{% set dbus_endpoint = salt['pillar.get']('databus:endpoint') %}
{% set dbus_access_key_id = salt['pillar.get']('databus:accessKeyId') %}
{% set dbus_access_key_secret = salt['pillar.get']('databus:accessKeySecret') %}
{% set dbus_access_key_secret_algorithm = salt['pillar.get']('databus:accessKeySecretAlgorithm') %}

{% set dbus_valid = databus_enabled and dbus_endpoint and dbus_access_key_id and dbus_access_key_secret %}

{% do databus.update({
    "enabled": databus_enabled,
    "endpoint": dbus_endpoint,
    "accessKeyId": dbus_access_key_id,
    "accessKeySecret": dbus_access_key_secret,
    "accessKeySecretAlgorithm": dbus_access_key_secret_algorithm,
    "valid": dbus_valid
}) %}