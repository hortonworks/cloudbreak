#!/bin/bash
: ${WORKSPACE=.}


VERSION=$(git describe --abbrev=0 --tags | cut -d '-' -f 1 | cut -d '.' -f 1,2)
git checkout -b "rc-$VERSION"
git push -u origin "rc-$VERSION"

echo "VERSION=${VERSION}" > $WORKSPACE/version