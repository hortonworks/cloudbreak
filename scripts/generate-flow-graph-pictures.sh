#!/bin/bash -e

dot_exists=$(which dot 2>/dev/null || echo 'FALSE');
if [ "${dot_exists}" = 'FALSE' ]; then
  echo "You need to install GraphViz dot CLI tool first" && exit 1
fi

find . -maxdepth 6 \
  -mindepth 2 \
  -type f \
  -name "*.dot" \
  -exec sh -xc '
filename=$0;
outputfile=${filename%.dot}.svg;
echo "creating file: ${outputfile}";
dot -Tsvg ${filename} > ${outputfile}
' {} \;