# MODUGO

An accessible mobility assistant app for everyone.

모두고는 Pleos Connect용 이동 지원 앱입니다. Android 프로젝트와 실행 안내는 [android/README.md](android/README.md), SDK 연동 범위와 검증 절차는 [SDK 연동 안내](android/docs/SDK_INTEGRATION.md)를 확인하세요.

## Android Studio에서 열기

1. Android Studio 왼쪽 위 메뉴에서 **File → Open**을 누릅니다.
2. 이 저장소 안의 `android` 폴더를 선택합니다. 현재 PC 경로는 `D:\MODUGO\MODUGO\android`입니다.
3. **OK**를 누르고 Gradle Sync가 끝날 때까지 기다립니다. Android SDK 경로는 해당 PC에 맞게 설정합니다.
4. 에뮬레이터 없이 확인하려면 **View → Tool Windows → Terminal**을 열고 아래 명령을 실행합니다. 현재 PC의 JDK 설치 경로를 사용하므로 다른 PC에서는 `JAVA_HOME`을 바꿉니다.

```powershell
cd D:\MODUGO\MODUGO\android
$env:JAVA_HOME = 'D:\Android_Studio\jbr'
.\gradlew.bat :app:assembleMockDebug :app:assembleRealDebug :app:testMockDebugUnitTest :app:testRealDebugUnitTest :app:lintMockDebug :app:lintRealDebug
```

`BUILD SUCCESSFUL`이면 빌드 및 로컬 검증이 완료됩니다. 실제 Pleos SDK 수신과 화면 동작은 지원 기기에서 별도로 확인해야 합니다.

이 저장소의 `android`는 기존 `D:\MODUGO\android` 프로젝트를 복사한 것입니다. 이후 Git으로 관리할 수정은 **이 저장소 안의 android 폴더**에서 하세요. 기존 폴더에서 수정한 내용은 자동으로 반영되지 않습니다.

APK, 빌드 캐시, `local.properties`, 서명 키는 저장소에 포함하지 않습니다. 기존 빌드 결과는 [SDK 빌드 보고서](android/docs/SDK_BUILD_REPORT.md)에 기록되어 있습니다.
