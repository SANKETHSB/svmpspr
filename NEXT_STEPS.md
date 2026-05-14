# ❗ CRITICAL: I Need the Backend Error Log

## The Situation

✅ MySQL is running  
✅ Database exists  
✅ Tables exist  
✅ Users were created by DataSeeder  
✅ Backend is running on port 8081  
❌ **Login returns 500 Internal Server Error**

The backend is **crashing** when you try to login, but the error is being caught and hidden.

---

## 🚨 URGENT: Check Your Backend Console

**Right now**, go to the terminal where you ran `mvn spring-boot:run`.

When you tried to login, the backend should have printed an error that looks like this:

```
19:XX:XX ERROR c.i.s.e.GlobalExceptionHandler - Unexpected error: 
java.lang.SomeException: Some error message here
    at com.infosys.svpms.service.impl.AuthServiceImpl.login(AuthServiceImpl.java:XX)
    at com.infosys.svpms.controller.AuthController.login(AuthController.java:XX)
    at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    ... (more lines)
```

**COPY THE ENTIRE ERROR** (from `ERROR` to the end of the stack trace) and share it with me.

---

## If You Don't See Any Error

### Option 1: Enable Debug Logging

1. **Stop the backend** (Ctrl+C)

2. **Edit** `backend/src/main/resources/application.properties`

3. **Add these lines:**
   ```properties
   logging.level.root=INFO
   logging.level.com.infosys.svpms=DEBUG
   logging.level.org.springframework.security=DEBUG
   logging.level.org.springframework.web.servlet.mvc.method.annotation=TRACE
   ```

4. **Restart backend:** `mvn spring-boot:run`

5. **Try login again**

6. **Look for ERROR messages** in the console

### Option 2: Test with curl/PowerShell

Run this command to test login:

```powershell
$body = '{"email":"admin@svpms.com","password":"Admin@123456"}'
Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" -Method POST -ContentType "application/json" -Body $body -UseBasicParsing
```

Then **immediately check the backend console** for errors.

---

## Possible Issues (Guessing Without Logs)

### Issue 1: Port Mismatch

Your backend might be running on port **8080** instead of **8081**.

**Check:**
```cmd
netstat -an | findstr "808"
```

If you see `:8080` instead of `:8081`, update your frontend `api.ts`:
```typescript
const api: AxiosInstance = axios.create({ baseURL: 'http://localhost:8080/api' });
```

### Issue 2: CORS Issue

The backend might be rejecting requests from the frontend.

**Check backend console** for:
```
WARN ... - CORS request rejected
```

**Fix:** Verify `SecurityConfig.java` has:
```java
cfg.setAllowedOrigins(List.of("http://localhost:3000"));
```

### Issue 3: Missing Bean

The `AuthenticationManager` or other bean might not be configured.

**Check backend startup logs** for:
```
ERROR ... - Bean 'authenticationManager' could not be found
```

### Issue 4: Database Transaction Issue

The audit log might be failing to save.

**Check backend console** for:
```
ERROR ... - Audit log failed
```

---

## Quick Diagnostic Commands

### 1. Verify User Exists
```cmd
mysql -u root -psam@2005 -e "USE svpms_db; SELECT * FROM users WHERE email='admin@svpms.com';"
```

### 2. Check Backend Port
```cmd
netstat -an | findstr ":808"
```

### 3. Test Backend Health
```powershell
Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" -Method OPTIONS
```

### 4. Check Application Properties
```cmd
type backend\src\main\resources\application.properties | findstr "server.port"
```

---

## What I Need From You

Please provide:

1. **The ERROR message from backend console** (MOST IMPORTANT!)
2. **The full stack trace** (all the `at ...` lines)
3. **What port is the backend actually running on?** (check with `netstat`)
4. **Did you change anything in the code?**

Without the actual error message, I'm just guessing blindly! 🎯

---

## Last Resort: Complete Reset

If nothing works, do a complete reset:

```cmd
REM 1. Stop backend (Ctrl+C)

REM 2. Drop database
mysql -u root -psam@2005 -e "DROP DATABASE IF EXISTS svpms_db;"

REM 3. Recreate database
mysql -u root -psam@2005 -e "CREATE DATABASE svpms_db;"

REM 4. Run schema
cd backend\src\main\resources
mysql -u root -psam@2005 < schema.sql

REM 5. Clean and rebuild backend
cd ..\..\..\..
mvn clean install -DskipTests

REM 6. Start backend
mvn spring-boot:run
```

Then try login again and **WATCH THE BACKEND CONSOLE** for errors!
