# Troubleshooting Login 500 Error

## Error Details
- **Endpoint**: POST http://localhost:8081/api/auth/login
- **Status**: 500 Internal Server Error
- **Location**: api.ts:24, AuthPage.tsx:105

## Common Causes & Solutions

### 1. Database Connection Issue (Most Likely)

**Check if MySQL is running:**
```bash
# Windows - Check MySQL service status
sc query MySQL80

# Or check if MySQL is listening on port 3306
netstat -an | findstr 3306
```

**Start MySQL if not running:**
```bash
# Windows - Start MySQL service
net start MySQL80
```

**Verify database exists:**
```sql
-- Connect to MySQL
mysql -u root -p

-- Check if database exists
SHOW DATABASES LIKE 'svpms_db';

-- If not exists, create it
CREATE DATABASE svpms_db;
```

**Verify credentials in application.properties:**
- Username: `root`
- Password: `sam@2005`
- Database: `svpms_db`
- Port: `3306`

### 2. Missing Database Tables

The application uses `spring.jpa.hibernate.ddl-auto=update`, which should auto-create tables. However, if the backend hasn't started successfully, tables won't exist.

**Check backend logs:**
```bash
# Navigate to backend directory
cd svpms\backend

# Run the backend with verbose logging
mvn spring-boot:run
```

**Look for these in the logs:**
- ✅ "Started SvpmsApplication" - Backend started successfully
- ❌ "Unable to open JDBC Connection" - Database connection failed
- ❌ "Access denied for user" - Wrong credentials
- ❌ "Unknown database" - Database doesn't exist

### 3. Backend Not Running

**Verify backend is running:**
```bash
# Check if port 8081 is in use
netstat -an | findstr 8081
```

**Start the backend:**
```bash
cd svpms\backend
mvn clean install
mvn spring-boot:run
```

### 4. Check Backend Console for Actual Error

The 500 error means the backend encountered an exception. Check the backend console/logs for the actual stack trace. Common errors:

**NullPointerException:**
- Missing required beans (AuthenticationManager, UserDetailsService)
- Database connection not initialized

**BadCredentialsException:**
- This should return 401, not 500, so if you see this in logs, there's a configuration issue

**DataAccessException:**
- Database connection issues
- Missing tables
- SQL syntax errors

## Step-by-Step Debugging

### Step 1: Verify MySQL is Running
```bash
# Windows
sc query MySQL80
# Should show "STATE: 4 RUNNING"
```

### Step 2: Test Database Connection
```bash
mysql -u root -psam@2005 -e "SELECT 1;"
# Should return: 1
```

### Step 3: Check if Database Exists
```bash
mysql -u root -psam@2005 -e "SHOW DATABASES LIKE 'svpms_db';"
# Should show: svpms_db
```

### Step 4: Start Backend with Logging
```bash
cd svpms\backend
mvn spring-boot:run
```

Watch for:
- Hibernate table creation logs
- "Started SvpmsApplication in X seconds"
- Any ERROR or WARN messages

### Step 5: Test Login Endpoint Directly
```bash
# Using curl (if available)
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"test@example.com\",\"password\":\"password123\"}"

# Or using PowerShell
Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"email":"test@example.com","password":"password123"}'
```

## Quick Fix Checklist

- [ ] MySQL service is running
- [ ] Database `svpms_db` exists
- [ ] Backend is running on port 8081
- [ ] No errors in backend console
- [ ] Tables are created (check backend logs for Hibernate DDL)
- [ ] At least one user exists in the database

## Creating a Test User

If tables exist but no users, you need to create one:

```sql
-- Connect to database
USE svpms_db;

-- Check if users table exists
SHOW TABLES LIKE 'user';

-- Check if any users exist
SELECT * FROM user;

-- If no users, you'll need to register through the signup flow
-- or insert a test user with BCrypt password
```

## Expected Backend Startup Logs

When backend starts successfully, you should see:
```
Hibernate: create table if not exists user (...)
Hibernate: create table if not exists vendor (...)
...
Started SvpmsApplication in 8.5 seconds
```

## Still Not Working?

1. **Check the exact error in backend console** - The 500 error is generic; the real error is in the backend logs
2. **Verify application.properties** - Ensure database credentials are correct
3. **Check firewall** - Ensure port 8081 is not blocked
4. **Try a different port** - Change `server.port=8081` to `server.port=8082` in application.properties

## Common Error Messages & Solutions

| Error Message | Solution |
|---------------|----------|
| "Access denied for user 'root'@'localhost'" | Wrong password in application.properties |
| "Unknown database 'svpms_db'" | Create database: `CREATE DATABASE svpms_db;` |
| "Communications link failure" | MySQL is not running |
| "Table 'svpms_db.user' doesn't exist" | Let backend start fully to create tables |
| "Bean 'authenticationManager' could not be found" | Configuration issue in SecurityConfig |

## Next Steps

1. Start MySQL service
2. Create database if needed
3. Start backend and watch logs carefully
4. Share the actual error from backend console if issue persists
