; Java App Launcher
;---------------------

; You want to change the below lines
Name "Process Scheduling"
Caption "Process Scheduling"
Icon "logo.ico"
OutFile "ProcessScheduling.exe"

; param below can be user, admin
RequestExecutionLevel user

SilentInstall silent
AutoCloseWindow true
ShowInstDetails show

Section ""
  ; command to execute
  StrCpy $0 'javaw -jar ProcessScheduling.jar'
  SetOutPath $EXEDIR
  Exec $0
SectionEnd
