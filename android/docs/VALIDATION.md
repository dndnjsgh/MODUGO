# 검증 결과

검증 기준일: **2026-11-14**

## 빌드와 자동 검사

| 항목 | 결과 |
| --- | --- |
| Mock Debug APK | 빌드 성공 |
| Real Debug APK | 빌드 성공 |
| Mock 단위 테스트 | 43개 통과 · 실패 0 · 오류 0 |
| Real 단위 테스트 | 공통 테스트 43개 통과 · 실패 0 · 오류 0 |
| Mock / Real Lint | 각각 오류 0 · 경고 26 |

두 변형에서 같은 공통 테스트를 실행한 결과입니다. 명령은 [빌드 안내](../README.md)에 정리되어 있습니다.

## 검증한 동작

| 영역 | 검증 내용 |
| --- | --- |
| 목적지 추천 | 필수 출입구 조건이 거리·시간보다 우선, 충족 후보 간 시간·거리 비교 |
| 정보 상태 | 미확인과 미충족 구분, 필수 조건을 꺼도 원래 시설 상태 유지 |
| 개인화 | 선호 변경에 따른 추천 순서·이유 변경, 선택 목적지의 조건 재확인 |
| 휴식 | 설정 시간 경계, 휴식 완료 후 초기화, 편의시설·우회 부담 비교 |
| 변경 확인 | 확인 전 미적용, 취소 시 유지, 주행·설정·여정 변경 후 재검증 |
| 통합 여정 | 목적지·경로 옵션·경유지 유지, 목적지에 맞는 도착·명령 안내 |
| 실제 연동 경계 | 실모드의 신규 경로 변경 차단, 기본 어댑터의 변경 요청 거절 |
| 오류 처리 | 연결 실패 시 값 제거, 잘못된 경로 수치 처리, 조작 제한 |

## Lint 경고

경고는 의존성 버전, target API, 사용하지 않는 리소스, Kotlin 확장 및 버전 카탈로그 사용 권고 등을 포함합니다. `ObsoleteLintCustomCheck`로 인해 현재 lifecycle 라이브러리의 일부 사용자 정의 검사는 실행되지 않았습니다. 경고를 억제하거나 검사 기준을 낮추지는 않았습니다.

## 검증 범위

- APK 빌드와 단위 테스트는 에뮬레이터 없이 수행했습니다.
- Compose Preview 코드는 두 변형에서 컴파일했습니다. 실제 Preview 렌더링과 화면 배치·터치 검수는 별도 확인 대상입니다.
- Pleos SDK 실제 수신, 차량별 Capability, 주행 UX 제한의 기기 통합 동작은 지원 환경에서 검증이 필요합니다.
- 시설·거리·ETA는 시연용 가상 데이터입니다. 현재 외부 시설·위치 서비스는 연결하지 않습니다.

## 보고서 위치

로컬 검증 후 다음 파일에서 세부 결과를 확인할 수 있습니다.

```text
app/build/reports/tests/testMockDebugUnitTest/index.html
app/build/reports/tests/testRealDebugUnitTest/index.html
app/build/reports/lint-results-mockDebug.html
app/build/reports/lint-results-realDebug.html
```

지원 기기에서의 후속 확인 항목은 [SDK 연동 범위](SDK_INTEGRATION.md)를 참조하세요.
