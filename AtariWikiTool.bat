@echo off
setlocal
cd "%~dp0"

java -jar out/AtariWikiTool.jar --convert --check ..
pause

