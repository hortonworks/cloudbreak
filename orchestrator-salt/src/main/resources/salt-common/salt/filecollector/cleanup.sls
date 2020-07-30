{%- from 'filecollector/settings.sls' import filecollector with context %}
filecollector_clean_dirs:
  cmd.run:
    - names:
{% if filecollector.destination == "LOCAL" %}
        - cdp-telemetry utils clean -d /var/lib/filecollector/ -p tmp/**
{% elif filecollector.destination == "ENG" %}
        - cdp-telemetry utils clean -d /var/lib/filecollector/
{% else %}
        - cdp-telemetry utils clean -d /var/lib/filecollector/ -p tmp/**
        - cdp-telemetry utils clean -d /var/lib/filecollector/ -p *.gz
{% endif %}