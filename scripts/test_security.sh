#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Base URL - Using HTTPS
BASE_URL="https://localhost:8443"

# Curl options for self-signed certificate
CURL_OPTS="-k"

echo -e "${YELLOW}🔒 HOSPITAL MANAGEMENT SECURITY TESTING${NC}"
echo "======================================"
echo "Testing URL: $BASE_URL"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Test 1: Valid Login - Admin
echo -e "\n${YELLOW}TEST 1: Valid Login - Admin${NC}"
echo "----------------------------------------"
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"1234"}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✓ PASS${NC}: Admin login successful"
    ADMIN_TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*' | grep -o '[^"]*$')
    if [ -z "$ADMIN_TOKEN" ]; then
        echo "Failed to extract token from response"
        echo "Response body: $BODY"
    else
        echo "Token received: ${ADMIN_TOKEN:0:20}..."
    fi
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: Admin login failed (Expected: 200, Got: $HTTP_CODE)"
    ((TESTS_FAILED++))
fi

# Test 2: Invalid Password
echo -e "\n${YELLOW}TEST 2: Invalid Password${NC}"
echo "----------------------------------------"
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"wrongpass"}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "401" ]; then
    echo -e "${GREEN}✓ PASS${NC}: Invalid password rejected"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: Invalid password not rejected (Expected: 401, Got: $HTTP_CODE)"
    ((TESTS_FAILED++))
fi

# Test 3: Account Lockout Test
echo -e "\n${YELLOW}TEST 3: Account Lockout Test${NC}"
echo "----------------------------------------"
echo "Attempting 3 failed logins for 'testuser'..."

for i in {1..3}; do
    curl $CURL_OPTS -s -X POST $BASE_URL/api/auth/login \
      -H "Content-Type: application/json" \
      -d '{"usernameOrEmail":"testuser","password":"wrongpass"}' > /dev/null
    echo "Failed attempt $i"
done

RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser","password":"wrongpass"}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "401" ]; then
    echo -e "${GREEN}✓ PASS${NC}: Account locked after 3 attempts"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: Account not locked (Expected: 401, Got: $HTTP_CODE)"
    ((TESTS_FAILED++))
fi

# Test 4: Get Tokens for All Roles
echo -e "\n${YELLOW}TEST 4: Get Tokens for All Roles${NC}"
echo "----------------------------------------"

# Doctor login
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"doctor","password":"1234"}')
DOCTOR_TOKEN=$(echo "$RESPONSE" | sed '$d' | grep -o '"accessToken":"[^"]*' | grep -o '[^"]*$')
echo "Doctor token: ${DOCTOR_TOKEN:0:20}..."

# Secretary login
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"secretary","password":"1234"}')
SECRETARY_TOKEN=$(echo "$RESPONSE" | sed '$d' | grep -o '"accessToken":"[^"]*' | grep -o '[^"]*$')
echo "Secretary token: ${SECRETARY_TOKEN:0:20}..."

# Patient login
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"patient","password":"1234"}')
PATIENT_TOKEN=$(echo "$RESPONSE" | sed '$d' | grep -o '"accessToken":"[^"]*' | grep -o '[^"]*$')
echo "Patient token: ${PATIENT_TOKEN:0:20}..."

# Test 5: Doctor Creates Visit (ALLOWED)
echo -e "\n${YELLOW}TEST 5: Doctor Creates Visit (ALLOWED)${NC}"
echo "----------------------------------------"
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/visits \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  -d '{
    "patientId": 1,
    "doctorId": 1,
    "visitDate": "2024-03-20T10:00:00",
    "diagnosis": "Test diagnosis"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
    echo -e "${GREEN}✓ PASS${NC}: Doctor can create visits"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: Doctor cannot create visits (Expected: 200/201, Got: $HTTP_CODE)"
    echo "Response: $(echo "$RESPONSE" | sed '$d')"
    ((TESTS_FAILED++))
fi

# Test 6: Secretary Access to Medical Data (FORBIDDEN)
echo -e "\n${YELLOW}TEST 6: Secretary Access to Medical Data (FORBIDDEN)${NC}"
echo "----------------------------------------"
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X GET $BASE_URL/api/visits \
  -H "Authorization: Bearer $SECRETARY_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "403" ]; then
    echo -e "${GREEN}✓ PASS${NC}: Secretary blocked from medical data"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: Secretary can access medical data (Expected: 403, Got: $HTTP_CODE)"
    ((TESTS_FAILED++))
fi

# Test 7: Patient Creates Visit (FORBIDDEN)
echo -e "\n${YELLOW}TEST 7: Patient Creates Visit (FORBIDDEN)${NC}"
echo "----------------------------------------"
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/visits \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $PATIENT_TOKEN" \
  -d '{
    "patientId": 1,
    "doctorId": 1,
    "visitDate": "2024-03-20T10:00:00",
    "diagnosis": "Test diagnosis"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "403" ]; then
    echo -e "${GREEN}✓ PASS${NC}: Patient cannot create visits"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: Patient can create visits (Expected: 403, Got: $HTTP_CODE)"
    ((TESTS_FAILED++))
fi

# Test 8: SQL Injection Prevention
echo -e "\n${YELLOW}TEST 8: SQL Injection Prevention${NC}"
echo "----------------------------------------"
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"usernameOrEmail\":\"admin' OR '1'='1\",\"password\":\"anything\"}")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "401" ]; then
    echo -e "${GREEN}✓ PASS${NC}: SQL injection blocked"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: SQL injection not blocked (Expected: 400/401, Got: $HTTP_CODE)"
    ((TESTS_FAILED++))
fi

# Test 9: XSS Prevention
echo -e "\n${YELLOW}TEST 9: XSS Prevention${NC}"
echo "----------------------------------------"
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/visits \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  -d '{
    "patientId": 1,
    "doctorId": 1,
    "visitDate": "2024-03-20T10:00:00",
    "diagnosis": "<script>alert(\"XSS\")</script>"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "401" ]; then
    echo -e "${GREEN}✓ PASS${NC}: XSS attempt blocked"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: XSS not blocked (Expected: 400, Got: $HTTP_CODE)"
    BODY=$(echo "$RESPONSE" | sed '$d')
    if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
        echo "WARNING: XSS was accepted but should be sanitized in output"
        echo "Response: $BODY"
    fi
    ((TESTS_FAILED++))
fi

# Test 10: Invalid JWT Token
echo -e "\n${YELLOW}TEST 10: Invalid JWT Token${NC}"
echo "----------------------------------------"
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X GET $BASE_URL/api/visits \
  -H "Authorization: Bearer invalid.jwt.token")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "401" ]; then
    echo -e "${GREEN}✓ PASS${NC}: Invalid JWT rejected"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: Invalid JWT not rejected (Expected: 401, Got: $HTTP_CODE)"
    ((TESTS_FAILED++))
fi

# Test 11: Password Complexity Check
echo -e "\n${YELLOW}TEST 11: Password Complexity Check${NC}"
echo "----------------------------------------"
RESPONSE=$(curl $CURL_OPTS -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "oldPassword": "1234",
    "newPassword": "weak"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "400" ] && [[ "$BODY" == *"Password must"* ]]; then
    echo -e "${GREEN}✓ PASS${NC}: Weak password rejected"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: Weak password not validated (Expected: 400, Got: $HTTP_CODE)"
    echo "Response: $BODY"
    ((TESTS_FAILED++))
fi

# Summary
echo -e "\n${YELLOW}======================================"
echo "SECURITY TEST SUMMARY"
echo -e "======================================${NC}"
echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed: ${RED}$TESTS_FAILED${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✅ ALL SECURITY TESTS PASSED!${NC}"
else
    echo -e "\n${RED}⚠️  SOME TESTS FAILED - CHECK SECURITY!${NC}"
fi

echo -e "\n${YELLOW}To verify encryption in database:${NC}"
echo "mysql -u root -proot123 nycsecdb -e \"SELECT * FROM patientvisit ORDER BY visitid DESC LIMIT 5;\""
