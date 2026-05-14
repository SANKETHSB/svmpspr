# Quick Fix for 500 Login Error

## The Problem
The backend is returning a 500 Internal Server Error when trying to login. This typically means:
- Database connection failed
- Database tables don't exist
- Backend encountered an unexpected exception

## Quick Fix Steps

### Step 1: Ensure MySQL is Running

```cmd
sc query MySQL80
```

If it says "STOPPED", start it:
```cmd
net start MySQL80
```

### Step 2: Initialize the Database

You have a `schema.sql` file. Let's use it to create the database and tables:

```cmd
cd c:\Users\hp\Desktop\svmpspro-main\svpms\backend\src\main\resources

mysql -u root -psam@2005 < schema.sql
```

This will:
- Create the `svpms_db` database
- Create all required tables (users, vendors, rfqs, etc.)
- Add necessary indexes

### Step 3: Verify Database Setup

```cmd
mysql -u root -psam@2005 -e "USE svpms_db; SHOW TABLES;"
```

You should see:
```
audit_logs
compliance_documents
notifications
purchase_orders
quotation_documents
quotation_items
quotations
rfq_attachments
rfq_items
rfq_revision_history
rfq_vendor_invites
rfqs
users
vendor_approval_history
vendors
```

### Step 4: Create a Test Admin User

Since you need to login, let's create a test admin user:

```cmd
mysql -u root -psam@2005 svpms_db
```

Then run this SQL:
```sql
-- Create admin user with password "admin123"
-- BCrypt hash for "admin123"
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
```

Exit MySQL:
```sql
EXIT;
```

### Step 5: Start the Backend

```cmd
cd c:\Users\hp\Desktop\svmpspro-main\svpms\backend

mvn clean install
mvn spring-boot:run
```

**Watch the console output!** Look for:
- ✅ "Started SvpmsApplication in X seconds" - Good!
- ❌ Any ERROR messages - Share these with me

### Step 6: Test Login

Once the backend shows "Started SvpmsApplication", try logging in with:
- **Email**: `admin@svpms.com`
- **Password**: `admin123`

## If It Still Fails

### Check Backend Console
When you try to login and get the 500 error, **immediately look at the backend console**. You'll see the actual exception. Common ones:

**"Table 'svpms_db.users' doesn't exist"**
- Solution: Run the schema.sql again (Step 2)

**"Access denied for user 'root'@'localhost'"**
- Solution: Check password in `application.properties`
- Current password: `sam@2005`

**"Communications link failure"**
- Solution: MySQL is not running (Step 1)

**"No AuthenticationProvider found"**
- Solution: Configuration issue - check SecurityConfig.java

### Enable More Logging

Edit `application.properties` and change:
```properties
logging.level.com.infosys.svpms=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

Then restart the backend to see detailed logs.

## Alternative: Let Hibernate Create Tables

If you don't want to use schema.sql, you can let Hibernate auto-create tables:

1. **Drop existing database** (if it has issues):
```cmd
mysql -u root -psam@2005 -e "DROP DATABASE IF EXISTS svpms_db; CREATE DATABASE svpms_db;"
```

2. **Change application.properties**:
```properties
spring.jpa.hibernate.ddl-auto=create
```

3. **Start backend** - Hibernate will create all tables

4. **Change back to update**:
```properties
spring.jpa.hibernate.ddl-auto=update
```

5. **Restart backend**

## Test User Credentials

After setup, you can login with:

**Admin User:**
- Email: `admin@svpms.com`
- Password: `admin123`

**Or register a new vendor** through the signup form.

## Still Not Working?

Share the **exact error message from the backend console** when you try to login. The 500 error is generic - the real error is in the backend logs.

Look for lines starting with:
- `ERROR`
- `Exception`
- `Caused by:`

Copy the full stack trace and share it.
