# Email Setup Guide for Hospital Management System

## Overview
This guide will help you set up email functionality using Mailtrap for testing email features in the Hospital Management System.

## What is Mailtrap?
Mailtrap is a safe email testing service that captures all emails sent from your application without actually delivering them to real email addresses. This is perfect for development and testing.

## Step 1: Create a Mailtrap Account

1. Visit [https://mailtrap.io](https://mailtrap.io)
2. Click on **"Sign Up"** 
3. Create a free account using your email address
4. Verify your email address through the confirmation email

## Step 2: Get Your Mailtrap Credentials

1. After logging in, you'll see the **Dashboard**
2. In the **Inboxes** section, click on your default inbox (usually named "My Inbox")
3. Click on **"Show Credentials"** or look for the SMTP settings
4. You'll see credentials like:
   - **Host**: sandbox.smtp.mailtrap.io
   - **Port**: 2525
   - **Username**: Something like `c3a0d5fd0a20e2`
   - **Password**: Something like `a1b2c3d4e5f6g7`

## Step 3: Configure the Application

### Option A: Using Environment Variables (Recommended)

1. Create a `.env` file in the project root directory:
```bash
cd /home/alexa/development/DEV6003FinalProject/DEV6003_Projects
touch .env
```

2. Add your Mailtrap credentials to the `.env` file:
```properties
# Email Configuration
MAIL_USERNAME=your_mailtrap_username_here
MAIL_PASSWORD=your_mailtrap_password_here
```

3. Replace `your_mailtrap_username_here` and `your_mailtrap_password_here` with your actual credentials from Mailtrap

### Option B: Direct Configuration (Not Recommended for Production)

If you prefer, you can directly edit `src/main/resources/application.properties`:
```properties
spring.mail.username=your_mailtrap_username_here
spring.mail.password=your_mailtrap_password_here
```

**Note**: Never commit credentials to version control!

## Step 4: Run the Application with Email

Use the provided script to run the application with email enabled:

```bash
./run-with-email.sh
```

Or manually:
```bash
source .env
export MAIL_ENABLED=true
mvn spring-boot:run
```

## Step 5: Test Email Features

1. **Registration Email**:
   - Go to https://localhost:8443/auth/register
   - Create a new account
   - Check your Mailtrap inbox for the welcome email

2. **Password Reset Email**:
   - Go to https://localhost:8443/auth/forgot-password
   - Enter a registered email address
   - Check Mailtrap for the password reset link

3. **Admin Password Reset**:
   - Login as admin
   - Go to Users management
   - Edit a user and change their password
   - The user will receive an email with temporary password

## Step 6: View Emails in Mailtrap

1. Go to [https://mailtrap.io](https://mailtrap.io)
2. Open your inbox
3. You'll see all emails sent by the application
4. Click on any email to view its content, headers, and HTML preview

## Troubleshooting

### Emails Not Appearing in Mailtrap?

1. **Check Credentials**: Ensure username and password are correct
2. **Check Logs**: Look for email-related errors in application logs
3. **Verify .env File**: Make sure the .env file is in the correct location
4. **Test Connection**: Try this curl command:
```bash
curl --ssl-reqd \
  --url 'smtp://sandbox.smtp.mailtrap.io:2525' \
  --user 'your_username:your_password' \
  --mail-from 'test@hospital.com' \
  --mail-rcpt 'test@test.com' \
  --upload-file - <<EOF
From: test@hospital.com
To: test@test.com
Subject: Test Email

This is a test email.
EOF
```

### Common Issues

1. **Port 2525 Blocked**: Some networks block port 2525. Try port 587 instead
2. **Authentication Failed**: Double-check your Mailtrap credentials
3. **SSL/TLS Issues**: Mailtrap supports STARTTLS on port 2525

## Security Notes

- **Never use Mailtrap in production** - it's only for testing
- **Keep credentials secure** - use environment variables
- **Don't commit .env files** - add `.env` to `.gitignore`
- **For production**, use a real email service like:
  - SendGrid
  - AWS SES
  - Mailgun
  - Your organization's SMTP server

## Application Features Using Email

1. **User Registration**: Sends welcome email with login instructions
2. **Password Reset**: Sends reset link valid for 1 hour
3. **Admin Password Reset**: Sends temporary password that must be changed on first login
4. **Account Lockout**: Notifies user when account is locked
5. **Security Alerts**: Sends notifications for suspicious activities

## Additional Configuration Options

You can customize email behavior in `application.properties`:

```properties
# Email templates location
spring.mail.templates.path=classpath:/templates/email/

# Email from address
spring.mail.from=noreply@hospital.com

# Email signature
spring.mail.signature=Hospital Management System Team
```

## For Instructors/Evaluators

To quickly test the email functionality:

1. Create a free Mailtrap account at https://mailtrap.io
2. Copy your credentials from the Mailtrap dashboard
3. Create `.env` file with your credentials
4. Run `./run-with-email.sh`
5. Test any email feature (register, forgot password, etc.)
6. Check your Mailtrap inbox to see the emails

The entire setup takes less than 5 minutes!
