# Eng Vocab

사진 속 영어 표현을 OpenAI로 분석해 개인 단어장으로 만들고 학습하는 Android 앱입니다. 단어장과 학습 기록은 기기 내부 Room 데이터베이스에 저장됩니다.

## 주요 기능

- 단어장 생성·수정·삭제
- 단어 직접 추가 및 OpenAI 뜻 추천
- 카메라 촬영 또는 사진 선택 후 영어 표현·뜻 추출
- 분석 결과 검토, 수정, 중복 처리 후 일괄 저장
- 전체·중요·미학습·복습 필요 단어 필터
- 앞/뒤 카드 학습과 학습 결과 기록
- Android Keystore로 보호되는 OpenAI API 키 저장
- 설정 화면에서 OpenAI 연결 상태 확인

## 실행 환경

- Android Studio 또는 JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0
- Android 16(API 36) 이상 기기

`minSdk`와 `targetSdk`는 36입니다. Android 17 실제 기기에서도 하위 호환으로 실행할 수 있습니다.

## 로컬 설정

1. 저장소를 복제합니다.

   ```powershell
   git clone https://github.com/yangisu/eng_vocab.git
   cd eng_vocab
   ```

2. Android SDK 경로를 저장소 루트의 `local.properties`에 설정합니다.

   ```properties
   sdk.dir=C\:\\Users\\사용자명\\AppData\\Local\\Android\\Sdk
   ```

   `local.properties`는 Git에 포함되지 않습니다.

3. 빌드와 단위 테스트를 실행합니다.

   ```powershell
   .\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
   ```

4. 휴대폰에서 개발자 옵션과 USB 디버깅을 켜고, PC 연결 후 표시되는 디버깅 허용 창을 승인합니다.

5. 연결 상태를 확인하고 디버그 APK를 설치합니다.

   ```powershell
   adb devices -l
   .\gradlew.bat connectedDebugAndroidTest
   adb install -r .\app\build\outputs\apk\debug\app-debug.apk
   ```

## OpenAI 설정

앱을 실행한 뒤 설정 화면에서 개인 OpenAI API 키를 저장하고 **OpenAI 연결 확인**을 누릅니다. 사진 분석과 뜻 추천에는 OpenAI API 사용 권한 및 결제 설정이 필요합니다. 이 앱은 `gpt-5.4-mini`와 Responses API를 사용합니다.

API 키는 소스 코드나 빌드 설정에 넣지 않습니다. 앱은 키를 Android Keystore로 암호화해 기기에 저장하지만, 개인용 클라이언트 앱만으로 키 탈취 가능성을 완전히 없앨 수는 없습니다. 별도 OpenAI 프로젝트의 키를 사용하고 낮은 사용 한도와 알림을 설정하는 것을 권장합니다.

## 실제 기기 점검 순서

1. 앱이 정상 실행되고 단어장을 생성할 수 있는지 확인합니다.
2. 설정에서 API 키를 저장한 뒤 연결 확인이 성공하는지 확인합니다.
3. 단어를 직접 추가하고 앱 재실행 후에도 남아 있는지 확인합니다.
4. 단어 입력 화면에서 OpenAI 뜻 추천이 동작하는지 확인합니다.
5. 카메라 권한을 허용하고 영어 단어가 있는 사진을 촬영합니다.
6. 사진 분석 결과를 수정하고 단어장에 저장합니다.
7. 사진 선택기로 기존 사진을 가져오는 흐름도 확인합니다.
8. 중복 단어의 건너뛰기·수정 처리가 동작하는지 확인합니다.
9. 학습 카드의 기억함·모름 결과가 필터와 복습 목록에 반영되는지 확인합니다.

카메라 권한 거부, 잘못된 API 키, 사용량 한도, 네트워크 오류는 각 화면에 한국어 안내로 표시됩니다.

## 데이터와 비용

- 단어장 데이터와 학습 기록은 기기 내부에만 저장됩니다.
- 선택하거나 촬영한 사진은 분석을 위해 OpenAI API로 전송됩니다.
- 임시 사진은 가져오기 완료 또는 취소 시 삭제됩니다.
- OpenAI API 호출 비용은 사용자의 OpenAI 계정에 청구됩니다.
