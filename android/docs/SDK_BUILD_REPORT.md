# SDK 연동 최종 검증 결과 — 2026-11-12

프로젝트: D:\MODUGO\android

최종 검증 명령:

```powershell
$env:JAVA_HOME = 'D:\Android_Studio\jbr'
.\gradlew.bat :app:assembleMockDebug :app:assembleRealDebug :app:testMockDebugUnitTest :app:testRealDebugUnitTest :app:lintMockDebug :app:lintRealDebug
```

결과: BUILD SUCCESSFUL (1분 42초).

| 항목 | Mock | 실제 SDK |
|---|---|---|
| APK 빌드 | 성공 | 성공 |
| 단위 테스트 | 25개 통과 | 동일한 25개 통과 |
| Lint | 오류 0, 경고 26 | 오류 0, 경고 26 |

경고는 의존성 버전·target API·기본 리소스·코딩 스타일 등이며 상세 내용은 app/build/reports/lint-results-mockDebug.html 및 lint-results-realDebug.html에 있습니다. 일부 기존 라이브러리의 자체 Lint 검사 호환 경고도 포함됩니다.

APK:
- app/build/outputs/apk/mock/debug/app-mock-debug.apk
- app/build/outputs/apk/real/debug/app-real-debug.apk

확인한 내용: 실제 SDK 클래스·콜백·데이터 타입의 컴파일 호환성, 경로 거리/시간 단위와 잘못된 값 처리, 미수신 상태에서 추천 중지, Mock 시나리오, 설정 잠금, 도착/체크리스트 초기화, 휴식 완료 처리, 두 flavor의 빌드 분리. 경로가 사라졌을 때 이전 도착 확인이 남는 문제도 수정하고 회귀 테스트를 통과했습니다.

실제 서비스와 연결한 시험은 수행하지 않았습니다. SDK 콜백의 기기별 실제 수신, 권한 승인, CarUxRestrictions 변화, 가로/세로 화면, 앱 재실행 시 설정 유지 등은 정상 부팅된 Pleos 에뮬레이터에서 확인해야 합니다. 테스트가 통과했다는 것은 서비스 연결 성공을 의미하지 않습니다.

실제 장소 검색 데이터/좌표가 없으므로 실제 경유지 전송은 아직 구현하지 않았습니다. 음성 SDK와 차량 장치 제어도 이번 연동 범위에 포함되지 않습니다. 상세 범위 및 버튼별 실행 절차는 [SDK_INTEGRATION.md](SDK_INTEGRATION.md)를 참고하세요.
