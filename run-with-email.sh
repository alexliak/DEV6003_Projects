#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Run Spring Boot with email enabled AND HTTPS
echo "Starting Hospital Management System with Mailtrap email..."
echo "Email Username: $MAIL_USERNAME"
echo "Email Enabled: $MAIL_ENABLED"
echo ""
echo "Running in HTTPS mode on port 8443 (SECURE)"
echo "Access at: https://localhost:8443"
echo ""
echo "Note: Browser will show security warning - click 'Advanced' and 'Proceed to localhost'"
echo ""

mvn spring-boot:run
