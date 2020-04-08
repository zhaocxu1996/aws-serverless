# serverless

## Prerequisites

Java 8, Maven installed.

## Build and Deploy instructions

run mvn clean package to build the jar file and zip it.

then use aws s3 cp --recursive command copy it to the s3 bucket.

finally, use aws lambda api.