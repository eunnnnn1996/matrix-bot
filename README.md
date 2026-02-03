# bithumb-trade-bot-pro

Java 17 + Spring Boot 3 프로젝트 (Gradle) - 빗썸 자동매매 기본 템플릿

## 실행
* Java 17 설치 필요
* Gradle 설치 또는 시스템에 gradle 명령어 필요 (gradlew wrapper jar는 포함되어 있지 않습니다)
* Oracle DB (또는 설정 변경)
* 실행: gradle bootRun

## 기본 DB 설정 (src/main/resources/application.yml)
jdbc:oracle:thin:@localhost:1521:xe
