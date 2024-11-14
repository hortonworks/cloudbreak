user-data-script-success:
  file.exists:
    - name: /var/cb-init-executed
    - failhard: True
