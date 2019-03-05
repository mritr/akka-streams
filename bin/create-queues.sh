#!/bin/bash

# Start goaws wherever you installed it.

queue_url=http://localhost:4100
queue_names="development-queue1 development-queue2 development-queue3 development-queue4 development-queue5"

for queue_name in $queue_names
  do
  aws --endpoint-url $queue_url sqs create-queue --queue-name $queue_name --attributes file://create-queue.json
done