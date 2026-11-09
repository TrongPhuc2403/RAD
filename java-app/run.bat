@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist bin\com\rad\center\ui\LanguageCenterApp.class (
    call build.bat
)
echo [RUN] Dang khoi chay He Thong Quan Ly Trung Tam Ngoai Ngu...
java -Dfile.encoding=UTF-8 -cp "bin;lib\mysql-connector-j-8.4.0.jar" com.rad.center.ui.LanguageCenterApp %*

