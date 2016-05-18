{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}

{% set ambari = {} %}
{% do ambari.update({
    'is_systemd' : is_systemd
}) %}