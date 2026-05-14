# 🔍 Capture the Actual Error

The backend is returning a generic 500 error, but the **real error is in the backend console**.

## Step 1: Look at Your Backend Console

Go to the terminal/command prompt where you ran `mvn spring-boot:run`.

When you tried to login just now, there should be **ERROR** messages that look like this:

```
ERROR ... - Unexpected error: 
java.lang.NullPointerException: Cannot invoke "..." because "..." is null
    at com.infosys.svpms.service.impl.AuthServiceImpl.login(AuthServiceImpl.java:XX)
    at com.infosys.svpms.controller.AuthController.login(AuthController.java:XX)
    ...
```

## Step 2: Copy the ENTIRE Stack Trace

Copy everything from `ERROR` to the end of the stack trace (all the `at ...` lines).

## Step 3: Share It With Me

Paste the error here so I can see exactly what's failing.

---

## Common Errors and Quick Fixes

### Error 1: NullPointerException in AuthServiceImpl
```
java.lang.NullPointerException: Cannot invoke "com.infosys.svpms.entity.User.getId()" because "u" is null
```
**Cause**: User not found in database
**Fix**: Verify user exists: `mysql -u root -psam@2005 -e "USE svpms_db; SELECT * FROM users WHERE email='admin@svpms.com';"`

### Error 2: Bean not found
```
NoSuchBeanDefinitionException: No qualifying bean of type 'org.springframework.security.authentication.AuthenticationManager'
```
**Cause**: SecurityConfig not properly configured
**Fix**: Check SecurityConfig.java for @Bean annotations

### Error 3: Database connection
```
CommunicationsException: Communications link failure
```
**Cause**: MySQL not running or wrong credentials
**Fix**: Check MySQL is running and credentials in application.properties

### Error 4: JwtUtil error
```
NullPointerException in JwtUtil.generateToken
```
**Cause**: JWT secret not configured
**Fix**: Check `jwt.secret` in application.properties

---

## Alternative: Enable More Detailed Logging

If you can't find the error, enable DEBUG logging:

1. Stop the backend (Ctrl+C)

2. Edit `backend/src/main/resources/application.properties`:
   ```properties
   logging.level.com.infosys.svpms=DEBUG
   logging.level.org.springframework.security=DEBUG
   logging.level.org.springframework.web=DEBUG
   ```

3. Restart backend: `mvn spring-boot:run`

4. Try login again

5. The console will show MUCH more detail about what's failing

---

## Quick Test: Check if User Exists

```cmd
mysql -u root -psam@2005 -e "USE svpms_db; SELECT id, name, email, role, is_active, is_locked FROM users WHERE email='admin@svpms.com';"
```

Should show:
```
+----+--------------+-----------------+-------+-----------+-----------+
| id | name         | email           | role  | is_active | is_locked |
+----+--------------+-----------------+-------+-----------+-----------+
|  X | System Admin | admin@svpms.com | ADMIN |         1 |         0 |
+----+--------------+-----------------+-------+-----------+-----------+
```

If the user doesn't exist or has `is_active=0` or `is_locked=1`, that's the problem!

---

## I Need to See:

1. **The ERROR message from backend console** (most important!)
2. **The full stack trace** (all the `at ...` lines)
3. **What you typed for email/password** (to verify it matches)

Without seeing the actual error, I'm just guessing! 🔍
