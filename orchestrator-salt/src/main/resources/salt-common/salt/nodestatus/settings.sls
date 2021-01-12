{% set nodestatus = {} %}
{% set server_username = salt['pillar.get']('nodestatus:serverUsername') %}
{% set server_password = salt['pillar.get']('nodestatus:serverPassword') %}

{% do nodestatus.update({
    "serverUsername": server_username,
    "serverPassword": server_password
}) %}