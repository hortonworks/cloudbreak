T_isSubPathPassed() {
  local result=$(is-sub-path /a/b /a/b/c)
  [[ $result == 0 ]]
}

T_isSubPathFailed() {
  local result=$(is-sub-path /a/b /c/d)
  [[ $result != 0 ]]
}
