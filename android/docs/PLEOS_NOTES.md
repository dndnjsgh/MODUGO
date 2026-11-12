> 이 문서는 최초 UI/Mock 구현 당시 조사입니다. 최신 SDK 연동 범위와 검증은 [SDK_INTEGRATION.md](SDK_INTEGRATION.md)를 참고하세요.

# Pleos Connect 적용 메모

확인일: 2026-11-11. 이번 범위는 1~3단계 UI와 Mock Mode이며 **실제 Pleos SDK는 연결하지 않았다.** 아래 SDK 이름과 흐름은 다음 단계의 검증 자료다.

## 개발 기준

- [빠른 시작](https://document.pleos.ai/docs/pleos-only/getting-started)은 Android SDK API 34, 제품 `minSdk` 28 이상을 안내한다. [에뮬레이터 설치](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)는 Gradle 8.0 이상과 API 34 기반 Pleos Emulator를 설명한다.
- 이 프로젝트는 기존 `compileSdk = 37`을 유지하고 `targetSdk = 34`, `minSdk = 28`을 사용한다. API 34 실행 환경이 compileSdk를 반드시 34로 제한한다는 의미는 아니다. 최신 Compose/AndroidX 의존성이 요구하는 컴파일 SDK를 강제로 낮추면 빌드 호환성이 깨질 수 있다. 실제 Pleos API 34 AVD에서의 실행 검증은 별도로 필요하다.
- [공식 Vehicle 샘플](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app)도 compile/target 35를 사용한다. 샘플의 min23은 샘플 기준이며 배포 제품의 min28 요구를 대체하지 않는다.

## 화면과 주행 UX

- [Display Layout](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout): 앱은 Application Window 안에서 실행된다. 시스템 Status Bar, Driving View, GNB를 앱에서 복제하지 않는다.
- [앱 구조 및 동작 모델](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model): 앱 창은 portrait/landscape 및 분할 화면에 대응해야 한다. 좁은 창에는 핵심 정보를 우선하며, 창 크기 변경·백그라운드 복귀·상태 복원을 고려한다.
- [디자인 가이드](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro): 핵심 정보는 1~2초 안에 읽을 수 있게 구성하고 텍스트·아이콘 대비 4.5:1 이상을 확보한다. 입력 응답은 0.25초 이내 또는 진행 상태를 표시한다. 주행 중 시선을 끄는 애니메이션과 자동 텍스트 스크롤을 피한다. 글자 물리 높이 기준은 주요 5mm·보조 3mm이므로 코드의 sp/dp를 공식 인증 규격으로 간주하지 않는다.
- [주행 규제 개발](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide): 실제 제품은 `CarUxRestrictionsManager`의 현재 값과 변경 콜백에서 제한을 반영해야 한다. 이번 정차/주행 상태와 조작 제한은 Mock 시뮬레이션이다.
- [지역별 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy): DO 미선언 앱은 기본적으로 주차 시 사용 가능·주행 시 사용 불가다. DO 선언만으로 모든 기능이 허용되는 것도 아니다. 실제 주행 사용 승인을 받았다고 표현하지 않는다.

## 다음 단계: SDK 연결 전 확인

1. **버전과 서비스 확인:** 포털에서 승인된 SDK, 설치 AAR 및 Pleos Emulator/Service 버전을 대조한다. [릴리즈 노트](https://document.pleos.ai/docs/pleos-only/release-note/sdk-release-note)의 NaviHelper 3.0.5에서는 좌표 인자 순서, 거리·시간 필드, 이벤트 타입이 바뀌었다. 오래된 예제의 클래스·메서드·권한을 추측해서 사용하지 않는다.
2. **NaviHelper:** [소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)의 초기화→리스너 등록→요청/조회→리스너 해제·리소스 해제 흐름을 실제 버전에 맞춰 구현한다. 경로 요청 결과와 상태는 Navi Service 이벤트로 받으며, 이벤트 가용성은 Navigation 앱의 상태에 따라 다를 수 있다. [연결 실패와 요청 오류](https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/error-handling)를 구분한다.
3. **NaviHelper 권한:** [권한 가이드](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/declaring-permissions)는 조회 `NAVI_ROUTE`, 경유지 등 경로 요청 `NAVI_ROUTE_SEARCH`, 내비 화면 요청 `NAVI_CUSTOM_MAP`으로 나눈다. 실제 API의 `@RequiresPermission`과 승인 범위를 확인한 뒤 필요한 것만 선언한다. Mock에서는 선언하지 않는다.
4. **Vehicle State 조회만:** [Capability](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/check-capability)의 접근 상태와 영역 지원을 확인하고 READ 또는 READ_WRITE가 허용된 항목만 조회한다. 차량 장치의 set 계열 제어는 구현하지 않는다. 사용할 수 없는 값·권한 오류·연결 오류·샘플 대체값을 구분한다.
5. **생명주기:** [공식 샘플](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app)은 초기화 반환만으로 연결 완료를 판단하지 말 것을 설명한다. 리스너 등록에 사용한 동일 객체·영역으로 해제하고, 콜백 처리 전에 SDK를 해제하지 않는다. foreground에서 필요한 구독만 유지하고 종료 시 리소스를 정리한다.
6. **출처 표시:** 샘플 경로, 차량 상태, 시설 정보, 버튼 기반 음성 명령은 모두 데모임을 유지한다. 실제 조회에 실패했을 때 샘플 데이터를 실제 센서값처럼 표시하지 않는다. TTS/STT·Gleo·FusedLocation·ADAS·외부 시설 API는 현재 연결 범위가 아니다.

## 공모전 맥락

[현대자동차그룹 공식 발표](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-kia-vehicle-app-development-contest)에 따르면 모빌리티 라이프 케어 등이 공모 분야이며, 2026-11-15까지 접수 후 11월 서류 심사와 12월 앱 시연 평가를 진행한다. 모두고는 이동 전→이동 중→휴식 추천→도착 준비의 사용자 경험을 시연한다. 이 Mock 데모만으로 실제 차량 기능 연동이나 심사 통과를 보장하지 않는다.

