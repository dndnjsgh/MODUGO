# 모두고(MODU:GO) — Mock / 실제 SDK 조회

차량 이동 전 준비 → 휴식 → 도착·하차를 지원하는 Kotlin/Compose 앱입니다. 1~3단계 데모에 4단계 실제 SDK 조회와 5단계 로컬 검증을 추가했습니다. 실제 서비스 수신은 Pleos 기기에서 별도 검증해야 합니다. 최신 범위와 피시방 검증 절차는 [SDK 연동 안내](docs/SDK_INTEGRATION.md)를 먼저 확인하세요.

## 실행

1. Android Studio에서 `D:\MODUGO\MODUGO\android`를 엽니다.
2. Gradle Sync 후 **Build → Select Build Variant** 또는 **View → Tool Windows → Build Variants**에서 `mockDebug`를 선택합니다. `app`을 실행합니다. 실제 조회를 검증할 때는 `realDebug`를 선택합니다.
3. Pleos Connect API 34 또는 일반 Android API 28 이상 기기/에뮬레이터에서 Run을 누릅니다.
4. 앱 이름은 **모두고 · Mock**입니다. 인터넷·SDK 키·Pleos Maven 접근 없이 실행됩니다(최초 Gradle 의존성 다운로드에는 인터넷 필요).

PowerShell:

```powershell
cd D:\MODUGO\MODUGO\android
$env:JAVA_HOME = 'D:\Android_Studio\jbr'
.\gradlew.bat :app:assembleMockDebug :app:testMockDebugUnitTest :app:lintMockDebug
```

APK: `app\build\outputs\apk\mock\debug\app-mock-debug.apk`

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r .\app\build\outputs\apk\mock\debug\app-mock-debug.apk
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.modugo/.MainActivity
```

기존 AGP 9.5.1 / Gradle 9.6.0 / Kotlin Compose 2.2.10 / Compose BOM 2026.02.01을 유지합니다. compileSdk 37은 기존 의존성 호환성을 위해 유지하고 targetSdk 34 / minSdk 28로 설정했습니다. `local.properties`의 SDK 경로와 JDK 경로는 각 PC 환경에 맞추세요.

## 구현 기능과 시연 순서

- 홈 및 5개 케어 화면, 설정: 한국어·민트 다크 테마, 창 너비에 따른 카드 배치, 큰 터치 영역.
- Smart Rest: 저장한 간격과 마지막 휴식 이후 이동 시간으로 추천. 샘플 장소 상세 및 앱 내부 경유지 추가.
- Predictive Journey: 목적지·거리·남은 시간과 상황 안내 자동 갱신.
- Voice Companion: 4개 명령 버튼으로 기능 실행 및 텍스트 응답. 마이크·TTS·STT 미사용.
- Arrival Care: 접근 안내, 정차 후 체크리스트와 확인, 명시된 샘플 접근성 시설.
- Comfort Care: 가상 온도·공조 상태, 선호 온도와 Motion Care 설정. 차량 제어 없음.
- 설정은 SharedPreferences로 재실행 후 유지. 주행 또는 상태 불명에서는 설정·상세 액션 차단.

화면의 **데모** 메뉴에서 정차, 주행 시작, 장시간 이동, 휴식 추천, 목적지 접근, 목적지 도착, SDK 연결 실패를 선택합니다. **조작**을 펼쳐 +10분 이동 또는 현재 여정을 유지한 정차 전환을 할 수 있습니다.

권장 시연:
1. 정차 → 설정에서 휴식 간격 및 Motion Care 설정.
2. 휴식 추천 → 케어 안내 → 조작/정차 전환 → 장소 상세 → 데모 경유지 추가.
3. 휴식 완료 → 추천 카운트다운 및 Motion Care 휴식 안내 초기화 확인.
4. 목적지 접근 → 도착 안내, 주행 중 상세 잠금 확인.
5. 목적지 도착 → 도착·하차 화면 → 체크리스트와 도착 안내 확인.
6. SDK 연결 실패 → 이전 차량·경로 값 제거 → 다른 시나리오로 복구.
7. 설정 변경 후 앱을 종료·재실행해 저장 확인.

시나리오 선택은 독립된 샘플 여정을 시작하므로 경유지·체크리스트·도착 확인을 초기화합니다. +10분은 시연 시간이며 실제 시계/센서와 연결되지 않습니다. 정차 전환 이후 다시 주행 시나리오를 선택하면 새 샘플 여정이 시작됩니다.

## 주요 파일

`app/src/main/java/com/example/modugo/` 기준:

| 파일 | 역할 |
|---|---|
| MainActivity.kt | Mock 의존성 구성, 생명주기 연결, 뒤로 이동 |
| ui/ModugoApp.kt, ModugoUiState.kt | 7개 화면과 상태·동작 계약 |
| ui/theme/* | 색상·글자·테마 |
| viewmodel/ModugoViewModel.kt | StateFlow 통합, 명령, 정차 액션 검증 |
| domain/Models.kt, CareEngine.kt | 모델, 휴식·도착·쾌적 안내 규칙 |
| data/MobilityRepository.kt | 공통 데이터 및 음성 명령 인터페이스 |
| mock/MockMobilityRepository.kt, MockVoiceCompanion.kt | 가상 상황·경유지·텍스트 응답 |
| settings/* | 설정 저장과 값 범위 검증 |
| sdk/PleosSources.kt | 향후 내비게이션 및 조회 전용 차량 상태 어댑터 계약 |

테스트는 `app/src/test`의 domain, mock, viewmodel에 있습니다. 기존 예제 테스트는 보존했습니다. 공식 가이드와 설계 근거는 [PLEOS_NOTES.md](docs/PLEOS_NOTES.md)를 참고하세요.

## 다음 단계

1. 승인된 NaviHelper/Vehicle artifact의 버전·클래스·API 권한·Capability를 실제 라이브러리와 대조합니다.
2. 공통 Repository 및 NavigationSource/VehicleStateSource에 실제 어댑터를 추가합니다. 연결 실패·null 값·리스너 해제를 처리하고 실데이터와 Mock 출처를 분리합니다.
3. CarUxRestrictionsManager와 실제 주행 정책을 연동하고 DO 심사 요건을 검토합니다. 현재 잠금은 Mock 시뮬레이션이며 실제 주행 승인이나 규제 연동이 아닙니다. DO 선언은 추가하지 않았습니다.
4. 검증된 시설 데이터 출처·갱신일·접근성 정보를 확보합니다. 현재 장소·거리·시설은 전부 샘플입니다.
5. 음성 SDK 권한 확보 후 텍스트 명령 구조에 TTS/STT를 연결합니다. 차량 제어는 이번 범위에 포함되지 않습니다.
6. 실제 Pleos 앱 창·분할화면·큰 글꼴·주야간·하드웨어 터치 크기를 검증합니다. 기본 화면은 다크 테마이며 주야간 자동 전환은 후속 작업입니다.

모드는 Build Variant(mockDebug/realDebug)로 선택합니다. 차량 제어·음성 인식/합성·위험 구간 예측·외부 시설 검색은 미구현입니다. 위 다음 단계 목록은 최초 Mock 구현 당시의 계획이며 최신 진행 상태는 SDK_INTEGRATION.md가 기준입니다.
