# 🔐 New Secure Credentials for DEV6003 Assessment

## Important: Change Password on First Login!

These are the NEW secure passwords that meet all complexity requirements:

| Username  | Old Password | NEW Password  | Role        | Email                  |
|-----------|--------------|---------------|-------------|------------------------|
| admin     | 1234         | Admin@2025!   | ADMIN       | admin@hospital.nyc     |
| doctor2   | -            | Doctor123!    | DOCTOR      | doctor2@hospital.com	  |
| user1     | -            | User123!      | PATIENT     | user1@mail.com        |
| secretary | 1234         | Secretary123! | SECRETARIAT | secr@doc.com |
| Jdoe      | -            | Jdoe123!      | PATIENT     | patient1@hospital.nyc  |
| patient2  | -            | Patient2#2025 | PATIENT     | patient2@hospital.nyc  |

## Password Requirements Met:
- ✅ At least 8 characters
- ✅ At least one uppercase letter
- ✅ At least one lowercase letter  
- ✅ At least one number
- ✅ At least one special character (!@#$%^&*())

## How to Change Password:

1. Login with OLD password (1234)
2. Go to User Menu → Change Password
3. Enter:
   - Current Password: 1234
   - New Password: [Use password from table above]
   - Confirm Password: [Same as new password]
4. Click "Change Password"

## For Testing:

1. First, update all passwords using the Change Password feature
2. Logout and login with new passwords to verify
3. The system enforces 90-day password expiry

## Security Notes:
- Passwords will be encrypted with BCrypt
- Failed login attempts are tracked
- Account locks after 3 failed attempts
- All password changes are logged
