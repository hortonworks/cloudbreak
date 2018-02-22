echo
echo "Runnning integration tests in container"
echo "======================================="
docker-compose run test

echo
echo "Failed tests:"
echo "============="
grep "not ok" test-result.tap || { echo " none " ; exit ; }
echo
exit 1
