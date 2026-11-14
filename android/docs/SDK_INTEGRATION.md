# Pleos SDK 연동 범위

## 빌드 변형

| 변형 | 데이터 제공부 | 용도 |
| --- | --- | --- |
| `mockDebug` | Mock Repository | 통합 추천과 UI 시연 |
| `realDebug` | NaviHelper 3.0.5 · Vehicle 2.3.2 | 지원 환경의 경로·차량 상태 조회 |

SDK 의존성은 `realImplementation`으로 구분합니다. Mock 빌드에는 Pleos SDK를 포함하지 않습니다. 두 변형은 `MobilityRepository` 계약과 같은 UI를 사용합니다.

## NaviHelper

- 목적지 이름, 남은 거리와 시간을 수신합니다.
- 거리 단위는 미터에서 km로, 시간은 초에서 분으로 변환합니다.
- 목적지 변경·취소·도착 이벤트를 반영합니다.
- 연결 및 데이터 수신 오류를 별도로 표시합니다.

목적지·경유지·경로 옵션 변경은 Mock에서만 처리합니다. 실제 SDK 조회 모드에서는 해당 변경 요청을 실행하지 않습니다.

`RouteOption.COMFORTABLE`은 공식 경로 옵션입니다. 현재 데모의 편안한 경로 선택은 이 옵션을 가정한 상태 변경이며, 실제 경로 재탐색이나 도로 구간별 진동·급커브·승차감 분석을 수행하지 않습니다.

## Vehicle State

- 앞좌석 좌측의 현재 온도와 공조 전원 상태를 조회합니다.
- 현재 주행 세션의 시간과 거리를 조회합니다.
- 각 항목의 Capability와 지원 좌석을 확인한 뒤 조회합니다.
- 수신하지 못한 값은 `—`로 표시하며, 주행 시간 정보가 없으면 시간 기반 휴식 추천을 중지합니다.

공조, 시트, 창문 등 차량 장치를 제어하는 API는 사용하지 않습니다. 휴식 완료는 앱 내부 기준 시간을 갱신하며 차량 상태를 변경하지 않습니다. 휴식 기준은 앱 프로세스 동안 유지되고 재실행 시 현재 주행 세션 기준으로 돌아갑니다.

## 수명주기와 주행 UX

화면이 활성화된 동안 5초 간격으로 조회하고, 15초 이상 갱신되지 않은 값을 제거합니다. 화면 종료 시 조회를 중단하고 리스너와 SDK 자원을 해제합니다. 종료된 세션의 늦은 콜백은 무시합니다.

실제 모드는 `CarUxRestrictions`가 baseline이고 distraction optimization이 요구되지 않을 때 상세 조작을 허용합니다. 제한 상태를 받지 못했거나 연결이 해제되면 조작을 잠급니다. 이 판단은 주차 기어나 차량 속도 측정을 의미하지 않으며, 앱은 DO 선언을 사용하지 않습니다.

## 권한 및 데이터 경계

- 직접 선언 권한: `NAVI_ROUTE`, `CAR_CLIMATE`, `CAR_DRIVE`
- SDK Manifest에서 병합되는 권한: `VEHICLE_BINDING`
- 차량 제어용 `CONTROL_CAR_*` 권한은 사용하지 않습니다.
- 검증된 시설·좌표 데이터가 없어 실모드의 휴식 후보는 비어 있습니다.
- 실제 경로 변경 연동에는 API 승인 범위, 유효한 목적지·경유지 좌표, 응답·실패 처리와 사용자 확인 흐름 검증이 필요합니다.
- TTS/STT, FusedLocation, Gleo Interface는 연동하지 않았습니다.

## 지원 환경에서의 통합 확인

1. Pleos 지원 기기 또는 공식 에뮬레이터에서 `realDebug`를 실행합니다.
2. 내비게이션에 목적지를 설정하고 목적지·거리·시간 수신을 확인합니다.
3. 목적지 변경·취소·도착 후 상태가 갱신되는지 확인합니다.
4. 지원되는 차량 상태를 변경해 온도·공조·주행 정보 조회를 확인합니다.
5. UX 제한 적용·해제에 따라 상세 조작이 잠기고 풀리는지 확인합니다.
6. 앱 재진입, 연결 해제, 지연 응답에서 재연결 및 오래된 값 제거를 확인합니다.

현재 검증은 SDK 클래스·콜백의 컴파일 호환성과 로컬 동작 테스트까지입니다. 실제 서비스 수신, 승인 권한 및 차종별 지원 여부는 위 통합 확인 대상입니다.

## 공식 자료

- [NaviHelper SDK 설정](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk)
- [NaviHelper 초기화·해제](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release)
- [NaviHelper 권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions)
- [경로 옵션](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/routes)
- [Vehicle SDK 설정](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup)
- [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)
