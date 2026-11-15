#!/bin/bash
echo "Inicializando cola SQS en LocalStack..."
awslocal sqs create-queue --queue-name hotel-reservation-events-queue --region us-east-1
echo "Cola SQS 'hotel-reservation-events-queue' creada con éxito."

