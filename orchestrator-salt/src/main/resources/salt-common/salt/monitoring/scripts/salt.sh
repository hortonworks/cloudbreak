#!/bin/bash

function get_memory_in_bytes() {
  mem_in_kb=$1
  mem_in_bytes=$(($mem_in_kb*1024))
  echo "$mem_in_bytes"
}

function get_ps_output() {
  local component=$1
  ps_out=$(ps aux | grep "bin/$component" | grep -v grep | awk {'sumCpu+=$3;sumMem+=$4;sumVsz+=$5;sumRss+=$6} END{print sumCpu,sumMem,sumVsz,sumRss;'})
  echo "$ps_out"
}

function get_top_output() {
  local component=$1
  top_out=$(top -n 1 -b -c | grep "bin/$component" | grep -v grep | awk {'sumCpu+=$9;sumMem+=$10} END{print sumCpu,sumMem;'})
  echo "$top_out"
}

function write_metric() {
  local metric_name=$1
  local metric_value=$2

  if [[ "$metric_name" != "" && "$metric_value" != "" ]]; then
    echo "$metric_name $metric_value" >> /var/lib/node_exporter/files/salt.prom.$$
  fi
}

if [[ -f /var/run/salt-master.pid ]]; then
  master_ps_out=$(get_ps_output "salt-master")
  master_top_out=$(get_top_output "salt-master")
  master_ps_cpu=$(echo $master_ps_out | cut -d ' ' -f1)
  master_top_cpu=$(echo $master_top_out | cut -d ' ' -f1)
  master_ps_memory=$(echo $master_ps_out | cut -d ' ' -f2)
  master_top_memory=$(echo $master_top_out | cut -d ' ' -f2)
  master_vsz=$(echo $master_ps_out | cut -d ' ' -f3)
  master_rss=$(echo $master_ps_out | cut -d ' ' -f4)
  master_vsz_in_bytes=$(get_memory_in_bytes $master_vsz)
  master_rss_in_bytes=$(get_memory_in_bytes $master_rss)
  write_metric "cb_salt_master_cpu_usage_ps_total" "$master_ps_cpu"
  write_metric "cb_salt_master_cpu_usage_top_total" "$master_top_cpu"
  write_metric "cb_salt_master_memory_usage_ps_total" "$master_ps_memory"
  write_metric "cb_salt_master_memory_usage_top_total" "$master_top_memory"
  write_metric "cb_salt_master_memory_vsz_bytes_total" "$master_vsz_in_bytes"
  write_metric "cb_salt_master_memory_rss_bytes_total" "$master_rss_in_bytes"
fi

if [[ -f /var/run/salt-minion.pid ]]; then
  minion_ps_out=$(get_ps_output "salt-minion")
  minion_top_out=$(get_top_output "salt-minion")
  minion_ps_cpu=$(echo $minion_ps_out | cut -d ' ' -f1)
  minion_top_cpu=$(echo $minion_top_out | cut -d ' ' -f1)
  minion_ps_memory=$(echo $minion_ps_out | cut -d ' ' -f2)
  minion_top_memory=$(echo $minion_top_out | cut -d ' ' -f2)
  minion_vsz=$(echo $minion_ps_out | cut -d ' ' -f3)
  minion_rss=$(echo $minion_ps_out | cut -d ' ' -f4)
  minion_vsz_in_bytes=$(get_memory_in_bytes $minion_vsz)
  minion_rss_in_bytes=$(get_memory_in_bytes $minion_rss)
  write_metric "cb_salt_minion_cpu_usage_ps_total" "$minion_ps_cpu"
  write_metric "cb_salt_minion_cpu_usage_top_total" "$minion_top_cpu"
  write_metric "cb_salt_minion_memory_usage_ps_total" "$minion_ps_memory"
  write_metric "cb_salt_minion_memory_usage_top_total" "$minion_top_memory"
  write_metric "cb_salt_minion_memory_vsz_bytes_total" "$minion_vsz_in_bytes"
  write_metric "cb_salt_minion_memory_rss_bytes_total" "$minion_rss_in_bytes"
fi

if [[ -f /var/lib/node_exporter/files/salt.prom.$$ ]]; then
  mv /var/lib/node_exporter/files/salt.prom.$$ /var/lib/node_exporter/files/salt.prom
fi

