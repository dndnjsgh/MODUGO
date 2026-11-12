# 검증 결과 — 2026-11-12

- 최종 경로: D:\MODUGO\android
- assembleDebug: 성공. app/build/outputs/apk/debug/app-debug.apk 생성.
- testDebugUnitTest: 20개 통과, 실패/오류 0개. CareEngine 8, Mock repository 7, ViewModel 4, 기존 예제 1.
- 실제 SDK·차량 제어·음성 SDK 의존성 및 권한: 추가하지 않음.
- 에뮬레이터: Pleos Connect API 34에 앞선 APK 설치 성공. 앱 실행 요청은 Activity service Broken pipe로 실패했고 CarServiceHelper 연결 실패/DeadSystemException 로그 확인. 일반 Android API 37 대체 실행도 기기 offline 상태로 화면 검증 미완료.
- lintDebug: 성공. 오류 0개, 경고 24개. 기존/추가 의존성 업데이트 권고, target API 34, 미사용 기본 리소스, 스타일 권고 등이 포함됩니다. 구버전 Lifecycle 자체 Lint 검사 일부는 API 호환 문제로 건너뛰었다는 경고가 있습니다. 전체 명령 BUILD SUCCESSFUL (12분 21초). 상세 결과: app/build/reports/lint-results-debug.html.

단위 테스트는 추천 임계값, 케어 비활성화, 오류 시 데이터 제거, 정차 액션 제한, 시나리오 초기화, 도착 확인, 명령 처리, 휴식 완료 초기화를 검증합니다. 실제 UI 조작, 설정의 프로세스 재시작 후 유지, portrait/landscape 렌더링은 기기가 정상 부팅된 뒤 README 시연 절차로 확인해야 합니다.

초기 한글 작업 경로는 AGP 경로 검사에 걸렸으며, 이후 요청한 영문 최종 경로에서 빌드했습니다. 이 경로 검사 우회 설정은 최종 프로젝트에 추가하지 않았습니다.

