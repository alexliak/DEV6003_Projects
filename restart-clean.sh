#!/bin/bash

echo "Stopping all Java processes..."
pkill -9 -f java
sleep 2

echo "Checking if port 8443 is free..."
if lsof -i :8443 > /dev/null 2>&1; then
    echo "Port 8443 is still in use. Killing process..."
    lsof -ti :8443 | xargs kill -9
    sleep 1
fi

echo "Port 8443 should be free now!"
echo ""

# Start the application
echo "Starting Hospital Management System..."
./run-with-email.sh
