#!/bin/bash

echo "🚀 Starting Payments Service on port 1293..."
cd "$(dirname "$0")/payments_service"
mvn spring-boot:run

