sudo -- bash -c \
"source activate_salt_env; \
salt-run jobs.list_jobs search_function=state.highstate | grep -E '^[0-9]{20}:$' | sed 's/.$//' > salt_jids_%s.txt; \
if [[ -s salt_jids_%s.txt ]]; then \
  while read jid; do \
    salt-run jobs.lookup_jid \$jid --out=json > salt_job_result_\$jid.json; \
  done < salt_jids_%s.txt; \
  zip salt_execution_metrics_%s.zip salt_jids_%s.txt salt_job_result_*.json; \
  chmod 744 salt_execution_metrics_%s.zip; \
fi;"