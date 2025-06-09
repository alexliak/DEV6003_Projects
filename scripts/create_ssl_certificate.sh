#!/bin/bash

# Create self-signed certificate for HTTPS
echo "Creating self-signed SSL certificate..."

# Create keystore directory
mkdir -p src/main/resources/keystore

# Generate certificate (non-interactive)
keytool -genkeypair \
  -alias hospital \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore src/main/resources/keystore/hospital.p12 \
  -validity 3650 \
  -storepass hospital123 \
  -keypass hospital123 \
  -dname "CN=localhost, OU=Hospital IT, O=NYC Hospital, L=Athens, ST=Attica, C=GR" \
  -ext "SAN=dns:localhost,ip:127.0.0.1" \
  2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ SSL certificate created successfully!"
    echo "Certificate location: src/main/resources/keystore/hospital.p12"
    echo "Certificate password: hospital123"
else
    echo "❌ Failed to create certificate"
fi
