
T_dockerClientversion() {
    result=$(docker-getversion "Docker version 1.5.0, build a8a31ef")
    [[ "$result" -eq 150 ]]
}

T_dockerClientVersionRC() {
    result=$(docker-getversion "Docker version 1.6.0-rc2, build c5ee149")
    [[ "$result" -eq 160 ]]
}

T_dockerServerVersion() {
    result=$(docker-getversion "Server version: 1.5.0-rc2")
    [[ "$result" -eq 150 ]]
}

T_dockerServerVersionRC() {
    result=$(docker-getversion "Server version: 1.6.0-rc2")
    [[ "$result" -eq 160 ]]
}

