Unicode true
ManifestDPIAware true
SetCompressor /SOLID lzma
RequestExecutionLevel admin

!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "nsDialogs.nsh"
!include "WinVer.nsh"
!include "x64.nsh"
!include "Sections.nsh"

Var Target
Var Dialog
Var Radio7
Var Radio10
Var Radio11
Var CompDialog
Var ChkVexel
Var ChkAdb
Var ChkDesktop
Var ChkStart


!define APP_NAME "DevAI Studio"
!define APP_VERSION "3.2.1"
!define APP_EXE "$INSTDIR\runtime\bin\javaw.exe"
!define APP_JAR "$INSTDIR\app\DevAIStudio-all.jar"

Name "${APP_NAME}"
OutFile "DevAI-Studio-Setup-${APP_VERSION}.exe"
InstallDir "$PROGRAMFILES64\DevAI Studio"
InstallDirRegKey HKLM "Software\DevAI Studio" "InstallDir"
BrandingText "DevAI Studio · Android + Vexel"

!define MUI_ABORTWARNING
!define MUI_ICON "icon.ico"
!define MUI_UNICON "icon.ico"
!define MUI_WELCOMEPAGE_TITLE "Установка DevAI Studio"
!define MUI_WELCOMEPAGE_TEXT "DevAI Studio — помощник разработчика для Android, 75 языков и Vexel.$\r$\n$\r$\nВ установщик входит Java Runtime 8, отдельная Java не нужна."
!define MUI_FINISHPAGE_RUN "${APP_EXE}"
!define MUI_FINISHPAGE_RUN_PARAMETERS "-jar $\"${APP_JAR}$\" --target=$Target"
!define MUI_FINISHPAGE_RUN_TEXT "Запустить DevAI Studio"
!define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\README-PC.txt"
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Открыть README"

!insertmacro MUI_PAGE_WELCOME
Page custom TargetPageCreate TargetPageLeave
!insertmacro MUI_PAGE_DIRECTORY
Page custom ComponentsPageCreate ComponentsPageLeave
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "Russian"



Function .onInit
  SetShellVarContext all
  SetRegView 64
  ${IfNot} ${RunningX64}
    MessageBox MB_ICONSTOP "DevAI Studio требует 64-битную Windows."
    Quit
  ${EndIf}
  StrCpy $Target 10
  ${If} ${IsWin7}
    StrCpy $Target 7
  ${EndIf}
  ReadRegStr $0 HKLM "SOFTWARE\Microsoft\Windows NT\CurrentVersion" "CurrentBuildNumber"
  ${If} $0 != ""
    IntOp $0 $0 + 0
    ${If} $0 >= 22000
      StrCpy $Target 11
    ${EndIf}
  ${EndIf}
FunctionEnd

Function TargetPageCreate
  !insertmacro MUI_HEADER_TEXT "Версия Windows" "Выберите целевую систему"
  nsDialogs::Create 1018
  Pop $Dialog
  ${If} $Dialog == error
    Abort
  ${EndIf}
  ${NSD_CreateLabel} 0 0 100% 30u "Приложение запускается на встроенной Java 8. Выберите версию Windows, чтобы включить подходящий режим оформления и совместимости."
  Pop $0
  ${NSD_CreateRadioButton} 0 38u 100% 14u "Windows 7 (64-bit)"
  Pop $Radio7
  ${NSD_CreateRadioButton} 0 58u 100% 14u "Windows 10 (64-bit)"
  Pop $Radio10
  ${NSD_CreateRadioButton} 0 78u 100% 14u "Windows 11 (64-bit)"
  Pop $Radio11
  ${If} $Target == 7
    ${NSD_SetState} $Radio7 ${BST_CHECKED}
  ${ElseIf} $Target == 11
    ${NSD_SetState} $Radio11 ${BST_CHECKED}
  ${Else}
    ${NSD_SetState} $Radio10 ${BST_CHECKED}
  ${EndIf}
  nsDialogs::Show
FunctionEnd

Function TargetPageLeave
  ${NSD_GetState} $Radio7 $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $Target 7
  ${EndIf}
  ${NSD_GetState} $Radio10 $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $Target 10
  ${EndIf}
  ${NSD_GetState} $Radio11 $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $Target 11
  ${EndIf}
FunctionEnd

Section "DevAI Studio (обязательно)" SecMain
  SectionIn RO
  SetShellVarContext all
  SetOutPath "$INSTDIR"
  File "icon.ico"
  File "README-PC.txt"
  SetOutPath "$INSTDIR\app"
  File "app\DevAIStudio-all.jar"
  File /r "app\languages\*.*"
  SetOutPath "$INSTDIR\runtime"
  File /r "runtime\*.*"
  SetOutPath "$INSTDIR"
  FileOpen $0 "$INSTDIR\target.ini" w
  FileWrite $0 "target=$Target$\r$\n"
  FileClose $0
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  WriteRegStr HKLM "Software\DevAI Studio" "InstallDir" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "DisplayName" "${APP_NAME}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "DisplayVersion" "${APP_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "Publisher" "DevAI Studio"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "DisplayIcon" "$INSTDIR\icon.ico"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "UninstallString" "$\"$INSTDIR\Uninstall.exe$\""
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio" "NoRepair" 1
SectionEnd

Section "Vexel APK (полная версия)" SecVexel
  SetOutPath "$INSTDIR"
  File "DevAI-Android.apk"
SectionEnd

Section "ADB для Vexel (полная версия)" SecAdb
  SetOutPath "$INSTDIR\platform-tools"
  File /r "platform-tools\*.*"
SectionEnd

Section "Ярлык на рабочем столе" SecDesktop
  CreateShortcut "$DESKTOP\DevAI Studio.lnk" "${APP_EXE}" "-jar $\"${APP_JAR}$\" --target=$Target" "$INSTDIR\icon.ico" 0
SectionEnd

Section "Ярлык в меню Пуск" SecStart
  CreateDirectory "$SMPROGRAMS\DevAI Studio"
  CreateShortcut "$SMPROGRAMS\DevAI Studio\DevAI Studio.lnk" "${APP_EXE}" "-jar $\"${APP_JAR}$\" --target=$Target" "$INSTDIR\icon.ico" 0
SectionEnd

Section "Ярлык Vexel" SecVexelShortcut
  SetOutPath "$INSTDIR"
  File "Vexel.cmd"
  SectionGetFlags ${SecVexel} $0
  IntOp $0 $0 & ${SF_SELECTED}
  SectionGetFlags ${SecAdb} $1
  IntOp $1 $1 & ${SF_SELECTED}
  ${If} $0 != 0
  ${AndIf} $1 != 0
    SectionGetFlags ${SecStart} $3
    IntOp $3 $3 & ${SF_SELECTED}
    ${If} $3 != 0
      CreateDirectory "$SMPROGRAMS\DevAI Studio"
      CreateShortcut "$SMPROGRAMS\DevAI Studio\Vexel.lnk" "$SYSDIR\cmd.exe" "/c $\"$INSTDIR\Vexel.cmd$\"" "$INSTDIR\icon.ico" 0
    ${EndIf}
    SectionGetFlags ${SecDesktop} $2
    IntOp $2 $2 & ${SF_SELECTED}
    ${If} $2 != 0
      CreateShortcut "$DESKTOP\Vexel.lnk" "$SYSDIR\cmd.exe" "/c $\"$INSTDIR\Vexel.cmd$\"" "$INSTDIR\icon.ico" 0
    ${EndIf}
  ${EndIf}
SectionEnd

Section "Uninstall"
  SetShellVarContext all
  SetRegView 64
  Delete "$DESKTOP\DevAI Studio.lnk"
  Delete "$DESKTOP\Vexel.lnk"
  Delete "$SMPROGRAMS\DevAI Studio\DevAI Studio.lnk"
  Delete "$SMPROGRAMS\DevAI Studio\Vexel.lnk"
  RMDir "$SMPROGRAMS\DevAI Studio"
  Delete "$INSTDIR\target.ini"
  Delete "$INSTDIR\icon.ico"
  Delete "$INSTDIR\README-PC.txt"
  Delete "$INSTDIR\DevAI-Android.apk"
  Delete "$INSTDIR\Vexel.cmd"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir /r "$INSTDIR\app"
  RMDir /r "$INSTDIR\runtime"
  RMDir /r "$INSTDIR\platform-tools"
  RMDir "$INSTDIR"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DevAI Studio"
  DeleteRegKey HKLM "Software\DevAI Studio"
SectionEnd


Function ComponentsPageCreate
  !insertmacro MUI_HEADER_TEXT "Компоненты" "Полная или выборочная установка"
  nsDialogs::Create 1018
  Pop $CompDialog
  ${If} $CompDialog == error
    Abort
  ${EndIf}
  ${NSD_CreateLabel} 0 0 100% 28u "Для полной версии нажмите «Полная версия» или оставьте все галочки. Vexel и ADB уже входят в установщик."
  Pop $0
  ${NSD_CreateCheckbox} 0 34u 100% 13u "Vexel APK — конструктор Vexel для телефона или эмулятора"
  Pop $ChkVexel
  ${NSD_CreateCheckbox} 0 54u 100% 13u "ADB — установка Vexel в эмулятор без Android SDK"
  Pop $ChkAdb
  ${NSD_CreateCheckbox} 0 74u 100% 13u "Ярлык на рабочем столе"
  Pop $ChkDesktop
  ${NSD_CreateCheckbox} 0 94u 100% 13u "Ярлык в меню «Пуск»"
  Pop $ChkStart
  ${NSD_SetState} $ChkVexel ${BST_CHECKED}
  ${NSD_SetState} $ChkAdb ${BST_CHECKED}
  ${NSD_SetState} $ChkDesktop ${BST_CHECKED}
  ${NSD_SetState} $ChkStart ${BST_CHECKED}
  ${NSD_CreateButton} 0 122u 92u 17u "Полная версия"
  Pop $0
  ${NSD_OnClick} $0 FullComponents
  ${NSD_CreateButton} 100u 122u 92u 17u "Только программа"
  Pop $0
  ${NSD_OnClick} $0 MinComponents
  nsDialogs::Show
FunctionEnd

Function ComponentsPageLeave
  ${NSD_GetState} $ChkVexel $0
  ${If} $0 == ${BST_CHECKED}
    SectionSetFlags ${SecVexel} ${SF_SELECTED}
  ${Else}
    SectionSetFlags ${SecVexel} 0
  ${EndIf}
  ${NSD_GetState} $ChkAdb $0
  ${If} $0 == ${BST_CHECKED}
    SectionSetFlags ${SecAdb} ${SF_SELECTED}
  ${Else}
    SectionSetFlags ${SecAdb} 0
  ${EndIf}
  ${NSD_GetState} $ChkDesktop $0
  ${If} $0 == ${BST_CHECKED}
    SectionSetFlags ${SecDesktop} ${SF_SELECTED}
  ${Else}
    SectionSetFlags ${SecDesktop} 0
  ${EndIf}
  ${NSD_GetState} $ChkStart $0
  ${If} $0 == ${BST_CHECKED}
    SectionSetFlags ${SecStart} ${SF_SELECTED}
  ${Else}
    SectionSetFlags ${SecStart} 0
  ${EndIf}
FunctionEnd

Function FullComponents
  ${NSD_SetState} $ChkVexel ${BST_CHECKED}
  ${NSD_SetState} $ChkAdb ${BST_CHECKED}
  ${NSD_SetState} $ChkDesktop ${BST_CHECKED}
  ${NSD_SetState} $ChkStart ${BST_CHECKED}
FunctionEnd

Function MinComponents
  ${NSD_SetState} $ChkVexel ${BST_UNCHECKED}
  ${NSD_SetState} $ChkAdb ${BST_UNCHECKED}
  ${NSD_SetState} $ChkDesktop ${BST_UNCHECKED}
  ${NSD_SetState} $ChkStart ${BST_CHECKED}
FunctionEnd
