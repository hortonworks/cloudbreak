include:
  - recipes.runner

{% set timeout = salt['pillar.get']('recipes:timeout') %}
create_recipe_log_dir_pre_service_deployment:
  file.directory:
    - name: /var/log/recipes/pre-service-deployment
    - makedirs: True

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% if args['pre-service-deployment'] is defined %}
{% for script_name in args['pre-service-deployment'] %}
/opt/scripts/pre-service-deployment/{{ script_name }}:
  file.managed:
     - source:
       - salt://pre-recipes/scripts/{{ script_name }}
       - salt://pre-recipes/scripts/pre-date.sh
     - makedirs: True
     - mode: 700

run_pre_service_deployment_script_{{ script_name }}:
  cmd.run:
    - name: /opt/scripts/recipe-runner.sh pre-service-deployment {{ script_name }}
    - onlyif:
      - test -f /opt/scripts/pre-service-deployment/{{ script_name }}
      - test ! -f /var/log/recipes/pre-service-deployment/{{ script_name }}.success
    - timeout: {{ timeout }}
{% endfor %}
{% endif %}

{% endif %}
{% endfor %}