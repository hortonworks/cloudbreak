load commands

@test "Check availability zone list" {
  CHECK_RESULT=$( availability-zone-list --credential testcred --region testregion )
  echo $CHECK_RESULT >&2
}