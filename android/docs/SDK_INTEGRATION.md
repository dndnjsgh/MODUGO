# 4~5단계 SDK 연동 및 검증

## 현재 구현
- `mockDebug`: 기존 가상 시나리오와 모든 케어 데모. Pleos SDK 의존성 없음.
- `realDebug`: NaviHelper 3.0.5와 Vehicle 2.3.2를 사용하는 실제 조회 빌드(`com.example.modugo`, 모두고 · SDK 조회).
- 실제 목적지 이름, 남은 거리(미터→km), 남은 시간(초→분 올림), 도착·취소·변경 이벤트를 반영합니다.
- Vehicle의 앞좌석 좌측 현재 온도(섭씨)와 공조 전원 상태를 조회합니다. 각 조회 전에 Capability의 READ/READ_WRITE와 좌석 지원을 확인합니다. 제어 API는 호출하지 않습니다.
- 화면이 보이는 동안 5초 간격 조회, 15초 이상 갱신되지 않은 값 제거, onStop에서 조회 중단/내비 리스너 해제/SDK release. 종료한 세션의 늦은 콜백은 무시합니다.
- CarUxRestrictions가 baseline이며 distraction optimization을 요구하지 않을 때만 설정·체크리스트 등 상세 조작을 허용합니다. 규제 상태 미수신/연결 해제 시 잠급니다. 이것은 주차 기어나 속도 측정이 아닙니다. DO 선언은 없습니다.
- 데이터 미지원·초기화 오류·조회 오류는 화면에 표시합니다. 샘플값으로 실제 수신 성공을 가장하지 않습니다.

## 데이터 가용성과 남은 범위
- 현재 주행 세션 시간(분)·거리(미터)는 Odometer Capability 확인 후 조회합니다. 수신된 주행 시간에 따라 Smart Rest/Motion Care 안내가 동작합니다. 미수신/미지원 값은 `—`이며 시간 추천을 중지합니다. 휴식 완료는 앱 내부 기준값만 기록하며 차량을 변경하지 않습니다. 휴식 기준값은 앱 프로세스 동안 유지되고 재실행 시 현재 Trip 시작 기준으로 돌아갑니다.
- 실제 장소 검색·좌표: 검증된 장소 데이터가 없으므로 실모드 휴식 장소는 비어 있고 실제 경유지 추가를 거절합니다. 가상 좌표를 생성하거나 NAVI_ROUTE_SEARCH 권한을 불필요하게 추가하지 않았습니다.
- 실제 시설 접근성, 음성 STT/TTS, 차량 장치 제어는 미연동입니다.
- 실모드와 Mock은 같은 패키지(com.example.modugo)입니다. 설치 시 서로 교체되며 저장한 사용자 설정을 공유합니다. 포털에 승인된 패키지명·서명과 일치하는지도 기기 검증 전에 확인하세요.

## 초보자용 빌드 순서 (에뮬레이터 불필요)
1. Android Studio에서 D:\MODUGO\MODUGO\android를 엽니다.
2. Gradle Sync가 끝날 때까지 기다립니다.
3. 아래 Terminal을 열고 PowerShell에서 입력합니다.

```powershell
cd D:\MODUGO\MODUGO\android
$env:JAVA_HOME = 'D:\Android_Studio\jbr'
.\gradlew.bat :app:assembleMockDebug :app:assembleRealDebug :app:testMockDebugUnitTest :app:testRealDebugUnitTest
```

APK:
- app/build/outputs/apk/mock/debug/app-mock-debug.apk
- app/build/outputs/apk/real/debug/app-real-debug.apk

SDK 다운로드에는 공식 Pleos Maven 접근이 필요합니다. SDK가 없는 PC에서도 Mock 의존성만으로 Mock 빌드를 할 수 있습니다.

## 피시방에서 확인할 순서
1. 동일 프로젝트와 Android SDK를 준비하고 Pleos AVD를 정상 부팅합니다.
2. Android Studio의 Build Variants 창에서 realDebug를 선택하고 상단 기기에서 Pleos를 고른 뒤 Run을 누릅니다.
3. Pleos 내비게이션에서 실제 테스트 목적지를 설정합니다. 모두고의 SDK 표식, 내비 연결 상태, 목적지·거리·시간 수신을 확인합니다.
4. 공식 Virtual Console에서 승인된 차량 상태를 변경하여 온도와 공조 읽기 값이 갱신되는지 확인합니다. 앱에는 차량 제어 버튼이 없습니다.
5. 목적지 변경·취소·도착 이벤트와 상태 갱신을 확인합니다.
6. CarUxRestrictions 제한 시 설정과 하차 조작이 비활성화되는지, 제한 해제 후 활성화되는지 확인합니다.
7. 앱을 나갔다 다시 열어 재연결, 서비스 불가 시 오류·미수신 표시, 오래된 값 제거를 확인합니다.
8. mockDebug로 바꾸어 원래 7개 시나리오, 가로/세로·분할 창, 설정 재시작 유지도 확인합니다.

에뮬레이터/실차 연결 검증은 이 PC에서 실행하지 않았습니다. 빌드 성공은 SDK 서비스 연결·승인 권한·차종 지원의 보장이 아닙니다.

## 코드와 확인 근거
- `app/src/mock/.../RepositoryFactory.kt`: Mock 주입
- `app/src/real/.../RepositoryFactory.kt`: 실제 Repository 주입
- `app/src/real/.../sdk/PleosMobilityRepository.kt`: SDK 초기화·이벤트·조회·해제
- `app/src/real/.../sdk/PlatformUx.kt`: 실제 플랫폼 주행 규제
- `app/src/main/.../sdk/RouteReading.kt`: 단위 변환 및 입력 검증
- `app/src/test/.../sdk/RouteReadingTest.kt`: 단위·경계·실제 모드 오표시 방지 검증

공식 Maven의 AAR를 javap로 확인하고 navi-data 3.0.4 sources의 미터/초 정의를 대조했습니다. 리플렉션으로 미확인 SDK API를 호출하지 않습니다.

실제 Manifest의 직접 권한은 NAVI_ROUTE, CAR_CLIMATE, CAR_DRIVE입니다. SDK Manifest에서 VEHICLE_BINDING도 병합됩니다. CONTROL_CAR_* 권한은 없습니다.

공식 문서:
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup
- https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app
- https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide
