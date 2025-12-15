user-data-script-success:
  file.exists:
    - name: /var/cb-init-executed

print-user-data-log:
  cmd.run:
    - name: cat /var/log/user-data.log && exit 1
    - onfail:
      - file: user-data-script-success
    - failhard: True