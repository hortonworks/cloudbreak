#!/bin/bash

function get_memory() {
  mem_in_kb=$(ps v $1 | tail -1 | tr -s " " | cut -d " " -f9)
  mem_in_bytes=$(($mem_in_kb*1024))
  echo "$mem_in_bytes"
}

function get_children_memory() {
  mem_in_kb=$(ps -o pid,rss --no-headers --ppid $1 | cut -d ' ' -f3 | awk 'NF{sum+=$1} END {print sum}')
  if [[ -z "${mem_in_kb// }" ]]; then
    echo "0"
  else
    mem_in_bytes=$(($mem_in_kb*1024))
    echo "$mem_in_bytes"
  fi
}

if [[ -f /var/run/salt-master.pid ]]; then
  master_pid=$(cat /var/run/salt-master.pid)
  mem_in_bytes=$(get_memory $master_pid)
  children_mem_in_bytes=$(get_children_memory $master_pid)
  echo "cb_salt_master_parent_memory_rss_bytes $mem_in_bytes" >> /var/lib/node_exporter/files/salt.prom.$$
  echo "cb_salt_master_children_memory_rss_bytes $mem_in_bytes" >> /var/lib/node_exporter/files/salt.prom.$$
fi

if [[ -f /var/run/salt-minion.pid ]]; then
  minion_pid=$(cat /var/run/salt-minion.pid)
  mem_in_bytes=$(get_memory $minion_pid)
  children_mem_in_bytes=$(get_children_memory $minion_pid)
  echo "cb_salt_minion_parent_memory_rss_bytes $mem_in_bytes" >> /var/lib/node_exporter/files/salt.prom.$$
  echo "cb_salt_minion_children_memory_rss_bytes $mem_in_bytes" >> /var/lib/node_exporter/files/salt.prom.$$
fi

if [[ -f /var/lib/node_exporter/files/salt.prom.$$ ]]; then
  mv /var/lib/node_exporter/files/salt.prom.$$ /var/lib/node_exporter/files/salt.prom
fi

