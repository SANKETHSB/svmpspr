# Fix Login 500 Error - Complete Guide

## 🚨 The Problem

You're getting a **500 Internal Server Error** when trying to login at:
```
POST http://localhost:8081/api/auth/login
```

This means the backend encountered an unexpected exception.

---

## ✅ Quick Fix (Automated)

### Option 1: Run the Setup Script

I've created an automated setup script for you:

```cmd
cd c:\Users\hp\Desktop\svmpspro-main\svpms
setup-database.bat
```

This will:
1. ✅ Check if MySQL is running (and start it if needed)
2. ✅ Create the database and all tables
3. ✅ Create a test admin user
4. ✅ Verify everything is set up correctly

**Test Credentials:**
- Email: `admin@svpms.com`
- Password: `admin123`

### Option 2: Run Diagnostics First

If you want to see what's wrong first:

```cmd
cd c:\Users\hp\Desktop\svmpspro-main\svpms
diagnose.bat
```

This will check:
- MySQL service status
- Database existence
- Tables existence
- User accounts
- Port availability

---

## 🔧 Manual Fix (Step by Step)

If you prefer to do it manually or the script fails:

### Step 1: Start MySQL

```cmd
net start MySQL80
```

### Step 2: Create Database and Tables

```cmd
cd c:\Users\hp\Desktop\svmpspro-main\svpms\backend\src\main\resources
mysql -u root -psam@2005 < schema.sql
```

### Step 3: Create Test Admin User

```cmd
mysql -u root -psam@2005 svpms_db
```

Then paste this SQL:
```sql
INSERT INTO users (name, email, password, role, is_active, failed_login_count, is_locked)
VALUES (
    'Admin User',
    'admin@svpms.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    TRUE,
    0,
    FALSE
);
EXIT;
```

### Step 4: Start Backend

```cmd
cd c:\Users\hp\Desktop\svmpspro-main\svpms\backend
mvn spring-boot:run
```

**Wait for this message:**
```
Started SvpmsApplication in X.XXX seconds
```

### Step 5: Test Login

Go to your frontend and login with:
- Email: `admin@svpms.com`
- Password: `admin123`

---

## 🐛 Still Getting 500 Error?

### Check Backend Console

When you try to login, **immediately look at the backend console**. You'll see the actual error. Look for:

```
ERROR ... Exception in thread ...
```

Common errors and solutions:

| Error Message | Solution |
|---------------|----------|
| `Table 'svpms_db.users' doesn't exist` | Run `schema.sql` again |
| `Access denied for user 'root'` | Check password in `application.properties` |
| `Communications link failure` | MySQL is not running |
| `Unknown database 'svpms_db'` | Create database: `CREATE DATABASE svpms_db;` |
| `No AuthenticationProvider found` | Configuration issue in SecurityConfig |

### Enable Debug Logging

Edit `backend/src/main/resources/application.properties`:

```properties
logging.level.com.infosys.svpms=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

Restart backend and try again. You'll see detailed logs.

---

## 📋 Verification Checklist

Run through this checklist:

- [ ] MySQL service is running (`sc query MySQL80`)
- [ ] Database `svpms_db` exists
- [ ] Tables are created (15 tables total)
- [ ] At least one user exists in `users` table
- [ ] Backend is running on port 8081
- [ ] Backend console shows "Started SvpmsApplication"
- [ ] No ERROR messages in backend console
- [ ] Frontend can reach `http://localhost:8081/api/auth/login`

---

## 🎯 Root Cause Analysis

The 500 error happens because:

1. **Database Connection Failed**
   - MySQL not running
   - Wrong credentials
   - Database doesn't exist

2. **Tables Don't Exist**
   - Schema not initialized
   - Hibernate couldn't create tables

3. **Authentication Configuration Issue**
   - Missing beans
   - Security misconfiguration

4. **No Users in Database**
   - Can't authenticate if no users exist
   - Need at least one user to test

---

## 🔍 How to Read Backend Logs

When you see the 500 error, look for this pattern in backend console:

```
ERROR 12345 --- [nio-8081-exec-1] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception

java.lang.NullPointerException: Cannot invoke "..." because "..." is null
    at com.infosys.svpms.service.impl.AuthServiceImpl.login(AuthServiceImpl.java:XX)
    ...
```

This tells you:
- **What failed**: NullPointerException
- **Where it failed**: AuthServiceImpl.login at line XX
- **Why it failed**: Something was null that shouldn't be

Share this stack trace if you need help!

---

## 🚀 Quick Start After Fix

Once everything is working:

### 1. Start Backend
```cmd
cd c:\Users\hp\Desktop\svmpspro-main\svpms\backend
mvn spring-boot:run
```

### 2. Start Frontend
```cmd
cd c:\Users\hp\Desktop\svmpspro-main\svpms\frontend
npm start
```

### 3. Login
- Go to `http://localhost:3000`
- Email: `admin@svpms.com`
- Password: `admin123`

### 4. Create More Users
Once logged in as admin, you can:
- Create more internal users (Procurement Manager, Compliance Officer)
- Approve vendor registrations
- Create RFQs

---

## 📞 Need More Help?

If you're still stuck, share:

1. **Output of `diagnose.bat`**
2. **Backend console logs** (especially ERROR lines)
3. **Browser console error** (F12 → Console tab)
4. **Network tab details** (F12 → Network → Click failed request → Response)

The 500 error is just a symptom - the real error is in the backend logs!

---

## 📚 Related Files

- `QUICK_FIX_500_ERROR.md` - Simplified version of this guide
- `TROUBLESHOOTING_LOGIN_500.md` - Detailed troubleshooting steps
- `setup-database.bat` - Automated setup script
- `diagnose.bat` - Diagnostic tool
- `schema.sql` - Database schema
- `application.properties` - Backend configuration

---

## ✨ Success Indicators

You'll know it's working when:

1. ✅ Backend console shows: `Started SvpmsApplication in X seconds`
2. ✅ No ERROR messages in backend console
3. ✅ Login returns 200 OK with JWT token
4. ✅ You're redirected to dashboard
5. ✅ Browser console shows: `Welcome back, Admin User!`

Good luck! 🎉
