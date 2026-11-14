# Android 빌드 및 실행

[프로젝트 소개](../README.md) · [시연 가이드](docs/INTEGRATED_DEMO.md) · [검증 결과](docs/VALIDATION.md)

## 개발 환경

| 항목 | 구성 |
| --- | --- |
| 언어·UI | Kotlin · Jetpack Compose |
| Android Gradle Plugin | 9.5.1 |
| Gradle Wrapper | 9.6.0 |
| Kotlin Compose 플러그인 | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Android SDK | compileSdk 37 · targetSdk 34 · minSdk 28 |
| 앱 패키지 | `com.example.modugo` |
| Gradle JVM | JDK 25 (`gradle-daemon-jvm.properties`) |

프로젝트의 Gradle JVM 설정을 지원하는 Android Studio와 Android SDK Platform 37을 준비합니다. 최초 의존성 다운로드에는 인터넷 연결이 필요합니다. `realDebug`는 공식 Pleos Maven 저장소에서 SDK를 받습니다.

## Android Studio

1. 저장소를 내려받습니다.

   ```sh
   git clone https://github.com/dndnjsgh/MODUGO.git
   ```

2. **File → Open**에서 저장소 내부의 **`android` 폴더**를 선택합니다.
3. Gradle Sync를 완료합니다. SDK 위치는 Android Studio에서 설정하며 `local.properties`는 저장소에 포함하지 않습니다.
4. **View → Tool Windows → Build Variants**에서 `app`의 변형을 선택합니다.

   | 변형 | 용도 |
   | --- | --- |
   | `mockDebug` | 가상 경로·시설·차량 상태를 사용하는 기능 시연 |
   | `realDebug` | 지원 Pleos 환경에서 SDK 조회 기능 확인 |

5. `mockDebug`는 Android API 28 이상 기기 또는 에뮬레이터에서 실행할 수 있습니다. 상단 실행 구성을 `app`으로 선택한 뒤 기기를 고르고 **Run**을 누릅니다.

두 변형은 같은 앱 패키지를 사용하므로 서로 교체 설치되며 저장한 설정을 공유합니다.

## 기기 없이 UI 확인

1. `mockDebug`를 선택합니다.
2. **Shift 두 번**으로 검색창을 열고 `ModugoPreviews.kt`를 찾습니다.
3. 편집기 오른쪽 위의 **Split** 또는 **Design**을 선택합니다.
4. **Build & Refresh**로 Preview를 생성합니다.
5. 버튼과 설정을 조작하려면 **Start Interactive Mode**를 사용합니다. **Run Preview**는 연결된 기기에 배포하는 기능입니다.

홈, 5개 케어, 설정 화면을 420dp·1280dp 크기로 제공하며, 목적지 비교에는 글자 배율 1.5배 Preview도 있습니다. Preview는 인메모리 상태를 사용하므로 기기 저장소나 SDK 연결이 필요하지 않습니다. Preview 종료 시 설정이 초기화됩니다.

## 명령줄 빌드

저장소의 `android` 폴더에서 실행합니다. 명령줄의 `JAVA_HOME`은 Android Studio의 Gradle JDK 설정과 호환되는 JDK를 가리켜야 합니다.

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleMockDebug :app:testMockDebugUnitTest :app:lintMockDebug --max-workers=2
```

macOS / Linux:

```sh
./gradlew :app:assembleMockDebug :app:testMockDebugUnitTest :app:lintMockDebug --max-workers=2
```

두 변형의 전체 로컬 검증:

```powershell
.\gradlew.bat :app:assembleMockDebug :app:assembleRealDebug :app:testMockDebugUnitTest :app:testRealDebugUnitTest :app:lintMockDebug :app:lintRealDebug --max-workers=2
```

APK 출력:

- `app/build/outputs/apk/mock/debug/app-mock-debug.apk`
- `app/build/outputs/apk/real/debug/app-real-debug.apk`

빌드 캐시, APK, 서명 키와 PC별 설정은 Git 추적에서 제외합니다.
