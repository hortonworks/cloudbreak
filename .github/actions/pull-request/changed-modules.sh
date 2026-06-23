#!/bin/bash
# Helper for the static-analysis PR jobs (checkstyle/spotbugs).
#
# Checkstyle and SpotBugs (effort=MIN) are per-module: a module's report depends
# only on that module's own sources/bytecode, never on its dependents. So a PR
# only needs to re-analyse the modules whose own files changed -- not the whole
# repo. This helper turns the PR's git diff into the matching Gradle task list.
#
# Usage:
#   source .github/actions/pull-request/changed-modules.sh
#   TASKS=$(changed_module_tasks spotbugsMain)
#   [ -z "$TASKS" ] && { echo "no module changed"; exit 0; }
#   ./gradlew $TASKS ...
#
# Falls back to the unscoped task name (= run for ALL modules) when a shared
# build/config file changes, because that can affect every module's result.

changed_module_tasks() {
  local task="$1"
  local base="${BRANCH:-master}"

  # Make sure the base branch tip is available for the merge-base diff.
  git fetch --no-tags --quiet origin "$base" 2>/dev/null || true

  local mergebase
  mergebase="$(git merge-base "origin/$base" HEAD 2>/dev/null)"
  [ -z "$mergebase" ] && mergebase="origin/$base"

  local files
  files="$(git diff --name-only "$mergebase" HEAD 2>/dev/null)"

  # No diff resolvable -> be safe and analyse everything.
  if [ -z "$files" ]; then
    echo "$task"
    return
  fi

  # Shared build/config touched -> a rule/dependency change can affect every
  # module, so analyse everything.
  if echo "$files" | grep -qE '^(build\.gradle|settings\.gradle|gradle\.properties|dependencies\.gradle|config/(checkstyle|spotbugs)/)'; then
    echo "$task"
    return
  fi

  # Otherwise emit one :module:task per changed Gradle module.
  local out="" m
  for m in $(echo "$files" | awk -F/ 'NF>1 {print $1}' | sort -u); do
    if [ -f "$m/build.gradle" ]; then
      out="$out :$m:$task"
    fi
  done
  echo "$out"
}
