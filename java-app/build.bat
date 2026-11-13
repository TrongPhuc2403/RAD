@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist bin mkdir bin
echo [BUILD] Dang bien dich ma nguon Java Swing (UTF-8)...
javac -encoding UTF-8 -cp "lib\mysql-connector-j-8.4.0.jar" -d bin src\com\rad\center\model\*.java src\com\rad\center\util\*.java src\com\rad\center\dao\*.java src\com\rad\center\service\*.java src\com\rad\center\server\*.java src\com\rad\center\ui\*.java
if %ERRORLEVEL% EQU 0 (
    echo [BUILD] Bien dich thanh cong!
) else (
    echo [BUILD] Co loi khi bien dich!
    exit /b 1
)

