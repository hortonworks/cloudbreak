#!/usr/bin/env bash

export RECIPE_TYPE=$1
export RECIPE_NAME=$2

echo "--------- Executing script: /opt/scripts/${RECIPE_TYPE}/${RECIPE_NAME} at $(date) ---------" >> /var/log/recipes/${RECIPE_TYPE}/${RECIPE_NAME}.log

bash -x /opt/scripts/${RECIPE_TYPE}/${RECIPE_NAME} 2>&1 | tee -a /var/log/recipes/${RECIPE_TYPE}/${RECIPE_NAME}.log

export EXIT_CODE=${PIPESTATUS[0]}

if [[ ${EXIT_CODE} -eq 0 ]]; then
  echo $(date) >> /var/log/recipes/${RECIPE_TYPE}/${RECIPE_NAME}.success
fi

echo "--------- Executed script: /opt/scripts/${RECIPE_TYPE}/${RECIPE_NAME} at $(date) EXIT CODE: ${EXIT_CODE} ---------" >> /var/log/recipes/${RECIPE_TYPE}/${RECIPE_NAME}.log

exit ${EXIT_CODE}
