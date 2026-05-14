@echo off
echo ============================================
echo SVPMS Database Setup Script
echo ============================================
echo.

echo [1/4] Checking MySQL service...
sc query MySQL80 | find "RUNNING" >nul
if %errorlevel% neq 0 (
    echo MySQL is not running. Starting MySQL...
    net start MySQL80
    if %errorlevel% neq 0 (
        echo ERROR: Failed to start MySQL. Please start it manually.
        pause
        exit /b 1
    )
) else (
    echo MySQL is already running.
)
echo.

echo [2/4] Creating database and tables...
cd /d "%~dp0backend\src\main\resources"
mysql -u root -psam@2005 < schema.sql
if %errorlevel% neq 0 (
    echo ERROR: Failed to create database. Check MySQL credentials.
    pause
    exit /b 1
)
echo Database created successfully!
echo.

echo [3/4] Verifying tables...
mysql -u root -psam@2005 -e "USE svpms_db; SHOW TABLES;"
echo.

echo [4/4] Creating test admin user...
mysql -u root -psam@2005 svpms_db -e "INSERT IGNORE INTO users (name, email, password, role, is_active, failed_login_count, is_locked) VALUES ('Admin User', 'admin@svpms.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', TRUE, 0, FALSE);"
if %errorlevel% equ 0 (
    echo Test admin user created!
    echo.
    echo ============================================
    echo Setup Complete!
    echo ============================================
    echo.
    echo You can now login with:
    echo   Email: admin@svpms.com
    echo   Password: admin123
    echo.
    echo Next steps:
    echo   1. Start the backend: cd backend ^&^& mvn spring-boot:run
    echo   2. Start the frontend: cd frontend ^&^& npm start
    echo   3. Login with the credentials above
    echo.
) else (
    echo Note: Admin user may already exist or there was an error.
)

pause
