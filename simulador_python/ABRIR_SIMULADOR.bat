@echo off
cd /d "%~dp0"
where py >nul 2>nul
if errorlevel 1 goto usar_python
py -3 simulador.py
goto terminar
:usar_python
python simulador.py
:terminar
if errorlevel 1 (
  echo.
  echo No se pudo abrir. Revisa INSTRUCCIONES.md y que Python tenga Tkinter.
  pause
)
