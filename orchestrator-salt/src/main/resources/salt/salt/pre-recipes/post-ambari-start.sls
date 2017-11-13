create_recipe_log_dir_post_start:
  file.directory:
    - name: /var/log/recipes

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}

# Recipes named "pre" are deprecated, but keeping them for backward compatibility
{% if args['pre'] is defined %}
{% for script_name in args['pre'] %}
/opt/scripts/pre/{{ script_name }}:
  file.managed:
     - source:
       - salt://pre-recipes/scripts/{{ script_name }}
       - salt://pre-recipes/scripts/pre-date.sh
     - makedirs: True
     - mode: 755

run_pre_script_{{ script_name }}:
  cmd.run:
    - name: sh -x /opt/scripts/pre/{{ script_name }} 2>&1 | tee -a /var/log/recipes/pre-{{ script_name }}.log && exit ${PIPESTATUS[0]}
    - onlyif:
      - ls /opt/scripts/pre/{{ script_name }}
    - unless: ls /var/log/recipes/pre-{{ script_name }}.log
{% endfor %}
{% endif %}

{% if args['post-ambari-start'] is defined %}
{% for script_name in args['post-ambari-start'] %}
/opt/scripts/post-ambari-start/{{ script_name }}:
  file.managed:
     - source:
       - salt://pre-recipes/scripts/{{ script_name }}
       - salt://pre-recipes/scripts/pre-date.sh
     - makedirs: True
     - mode: 755

run_post_ambari_start_script_{{ script_name }}:
  cmd.run:
    - name: sh -x /opt/scripts/post-ambari-start/{{ script_name }} 2>&1 | tee -a /var/log/recipes/post-ambari-start-{{ script_name }}.log && exit ${PIPESTATUS[0]}
    - onlyif:
      - ls /opt/scripts/post-ambari-start/{{ script_name }}
    - unless: ls /var/log/recipes/post-ambari-start-{{ script_name }}.log
{% endfor %}
{% endif %}

{% endif %}
{% endfor %}