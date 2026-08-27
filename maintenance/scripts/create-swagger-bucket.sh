#!/bin/bash -e
#
# One-time setup for the maintenance OpenAPI baseline bucket.
# Run with credentials that can create S3 buckets in us-east-2 (RE/ops).
#
# After creation, scripts/build-dev.sh uploads:
#   s3://maintenance-swagger/openapi-${VERSION}.json

set -euo pipefail

BUCKET=maintenance-swagger
REGION=us-east-2

echo "Creating bucket s3://${BUCKET} in ${REGION} (skip if it already exists)..."
aws s3api create-bucket \
  --bucket "${BUCKET}" \
  --region "${REGION}" \
  --create-bucket-configuration "LocationConstraint=${REGION}" \
  2>/dev/null || echo "Bucket may already exist; continuing."

echo "Applying public read policy for openapi-*.json objects..."
POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::${BUCKET}/*"
    }
  ]
}
EOF
)
aws s3api put-bucket-policy --bucket "${BUCKET}" --policy "${POLICY}"

echo "Done. Baseline URL pattern:"
echo "  https://${BUCKET}.s3.${REGION}.amazonaws.com/openapi-<VERSION>.json"
