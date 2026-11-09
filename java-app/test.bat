@echo off
chcp 65001 >nul
cd /d "%~dp0"
call build.bat
echo [TEST] Dang chay Kiem thu Tich hop TC-01 -> TC-10 va Xuat Hinh Anh Giao Dien...
java -Dfile.encoding=UTF-8 -cp "bin;lib\mysql-connector-j-8.4.0.jar" com.rad.center.ui.TestRunnerAndScreenshotGenerator

