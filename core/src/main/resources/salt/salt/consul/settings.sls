
{%- set advertise_addr = salt['grains.get']('consul:advertise_addr') %}
{%- set is_server = salt['grains.get']('consul:server', 'False') %}
{%- set node_name = salt['grains.get']('host') %}
{%- set recursors = salt['grains.get']('consul:recursors', '8.8.8.8') %}
{%- set bootstrap_expect = salt['pillar.get']('consul:bootstrap_expect', 1) %}
# {%- set retry_join = salt['pillar.get']('consul:retry_join') %}
{%- set retry_join = salt['grains.get']('consul:server_addr') %}

{%- set consul = {} %}
{%- do consul.update({
    'advertise_addr': advertise_addr,
    'is_server': is_server,
    'node_name': node_name,
    'recursors': recursors,
    'bootstrap_expect': bootstrap_expect,
    'retry_join': retry_join
}) %}