# Forgot Password Feature - User Guide

## 🔍 Where to Find the "Forgot Password?" Link

### Location

The "Forgot Password?" link is located on the **Login page**:

- **Position**: Below the password field, above the "Log In" button
- **Alignment**: Right-aligned
- **Color**: Purple/Blue (#667eea)
- **Text**: "Forgot Password?"

### Visual Layout

```
┌─────────────────────────────────────┐
│  Log In                             │
│  Enter your credentials to get      │
│  started.                           │
│                                     │
│  Email Address                      │
│  [you@company.com            ]      │
│                                     │
│  Password                           │
│  [••••••••••••••••          ] 👁    │
│                                     │
│                  Forgot Password? ← HERE
│                                     │
│  [        Log In        ]           │
│                                     │
│  Don't have an account? Sign up     │
└─────────────────────────────────────┘
```

## 🚀 How to Use Password Reset

### Step 1: Access Forgot Password

1. Go to http://localhost:3000/login
2. Click the **"Forgot Password?"** link (below password field)

### Step 2: Request OTP

1. Enter your email address
2. Click **"Send OTP"** button
3. Wait for success message: "OTP sent! Check console for OTP code."

### Step 3: Check Backend Console

1. Open the terminal/console where backend is running
2. Look for the OTP output:

```
============================================================
PASSWORD RESET OTP FOR: admin@svpms.com
OTP CODE: 965451
VALID UNTIL: 2026-05-06T18:40:07
============================================================
```

### Step 4: Reset Password

1. Enter the **6-digit OTP** from console
2. Enter your **new password** (minimum 8 characters)
3. **Confirm** your new password
4. Click **"Reset Password"** button

### Step 5: Login with New Password

1. You'll be redirected to the login page
2. Enter your email and **new password**
3. Click **"Log In"**

## 🔧 Troubleshooting

### "Forgot Password?" Link Not Visible

**Solution 1: Hard Refresh Browser**

- Windows/Linux: Press **Ctrl + Shift + R** or **Ctrl + F5**
- Mac: Press **Cmd + Shift + R**

**Solution 2: Clear Browser Cache**

1. Open Developer Tools (F12)
2. Right-click the refresh button
3. Select "Empty Cache and Hard Reload"

**Solution 3: Check Browser Console**

1. Press F12 to open Developer Tools
2. Go to Console tab
3. Look for any JavaScript errors
4. If errors exist, report them

**Solution 4: Verify Frontend is Running**

- Check that frontend is running on http://localhost:3000
- Look for "webpack compiled" message in terminal

### OTP Not Appearing in Console

**Check Backend Console**

- Make sure you're looking at the **backend** terminal (not frontend)
- Backend runs on port 8081
- Look for lines starting with "PASSWORD RESET OTP FOR:"

### Invalid OTP Error

**Common Causes:**

1. **OTP Expired**: OTP is valid for 10 minutes only
2. **Wrong OTP**: Make sure you copied all 6 digits correctly
3. **Old OTP**: If you requested multiple OTPs, use the latest one

**Solution**: Request a new OTP and try again

### Password Reset Failed

**Check Password Requirements:**

- Minimum 8 characters
- Passwords must match (password and confirm password)

## 📝 Test Accounts

You can test password reset with these accounts:

| Email                 | Current Password | Role                |
| --------------------- | ---------------- | ------------------- |
| admin@svpms.com       | Admin@123456     | Admin               |
| manager@svpms.com     | Manager@123456   | Procurement Manager |
| vendor@techsupply.com | Vendor@123456    | Vendor (Approved)   |

## ✅ Success Indicators

### OTP Request Success

- ✅ Toast message: "OTP sent! Check console for OTP code."
- ✅ Form switches to OTP entry mode
- ✅ OTP appears in backend console

### Password Reset Success

- ✅ Toast message: "Password reset successfully! You can now login."
- ✅ Redirected to login page
- ✅ Can login with new password

## 🎯 Quick Test

**Test the feature in 2 minutes:**

1. Open http://localhost:3000/login
2. Click "Forgot Password?" (if not visible, hard refresh: Ctrl+Shift+R)
3. Enter: admin@svpms.com
4. Click "Send OTP"
5. Check backend console for OTP (6 digits)
6. Enter OTP and new password (e.g., "NewPass@123")
7. Click "Reset Password"
8. Login with admin@svpms.com / NewPass@123

## 📞 Support

If you continue to have issues:

1. Check both frontend and backend are running
2. Clear browser cache completely
3. Check browser console for errors (F12)
4. Verify you're on http://localhost:3000/login (not /register)
5. Try a different browser

## 🔐 Security Notes

- OTP is valid for **10 minutes** only
- OTP is printed to console (no email in development)
- Account is automatically unlocked after successful password reset
- Failed login counter is reset after password reset
- All password reset actions are logged in audit trail
