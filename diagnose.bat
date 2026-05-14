@echo off
echo ============================================
echo SVPMS Diagnostic Tool
echo ============================================
echo.

echo [CHECK 1] MySQL Service Status
echo ----------------------------------------
sc query MySQL80 | find "STATE"
echo.

echo [CHECK 2] MySQL Port 3306
echo ----------------------------------------
netstat -an | findstr ":3306"
if %errorlevel% neq 0 (
    echo WARNING: MySQL is not listening on port 3306
) else (
    echo OK: MySQL is listening on port 3306
)
echo.

echo [CHECK 3] Backend Port 8081
echo ----------------------------------------
netstat -an | findstr ":8081"
if %errorlevel% neq 0 (
    echo WARNING: Backend is not running on port 8081
) else (
    echo OK: Backend is running on port 8081
)
echo.

echo [CHECK 4] Database Exists
echo ----------------------------------------
mysql -u root -psam@2005 -e "SHOW DATABASES LIKE 'svpms_db';" 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Cannot connect to MySQL or database doesn't exist
) else (
    echo OK: Database svpms_db exists
)
echo.

echo [CHECK 5] Tables Exist
echo ----------------------------------------
mysql -u root -psam@2005 -e "USE svpms_db; SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'svpms_db';" 2>nul
echo.

echo [CHECK 6] Users in Database
echo ----------------------------------------
mysql -u root -psam@2005 -e "USE svpms_db; SELECT COUNT(*) as user_count FROM users;" 2>nul
mysql -u root -psam@2005 -e "USE svpms_db; SELECT id, name, email, role FROM users LIMIT 5;" 2>nul
echo.

echo [CHECK 7] Vendors in Database
echo ----------------------------------------
mysql -u root -psam@2005 -e "USE svpms_db; SELECT COUNT(*) as vendor_count FROM vendors;" 2>nul
mysql -u root -psam@2005 -e "USE svpms_db; SELECT id, company_name, email, status FROM vendors LIMIT 5;" 2>nul
echo.

echo ============================================
echo Diagnostic Complete
echo ============================================
echo.
echo If any checks failed, run: setup-database.bat
echo.
pause
