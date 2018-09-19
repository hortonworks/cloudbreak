package utils

import (
	"time"

	log "github.com/Sirupsen/logrus"
)

func TimeTrack(start time.Time, name string) {
	elapsed := time.Since(start)
	log.Infof("%s took %s", name, elapsed)
}
