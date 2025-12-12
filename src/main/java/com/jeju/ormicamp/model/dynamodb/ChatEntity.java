package com.jeju.ormicamp.model.dynamodb;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

@Getter
@Setter
@ToString
@DynamoDbBean // "이 클래스는 DynamoDB 테이블입니다"라고 알려주는 표시
public class ChatEntity {

    // 1. 기본 키 (PK, SK) - 이름을 일반화시킴
    private String pk; // 여기에 "USER#id" 또는 "SESSION#id"가 들어감
    private String sk; // 여기에 "SESSION#ts" 또는 "MSG#ts"가 들어감

    // 2. 데이터 구분용
    private String type; // "META", "CHAT", "PLAN"

    // 3. 채팅용 속성
    private String role;    // "USER", "AI"
    private String content; // "맛집 추천해줘"

    // 4. 여행 목록(Meta)용 속성
    private String sessionTitle; // "제주 3박 4일 여행"
    private String startDate; // 여행 시작 날짜 "2025-12-12"
    private String endDate; // 여행 끝 날짜 "2025-12-15"
    private String capacity; // 인원 수 "3"


    // 5. 플래너용 속성 (복잡한 객체 리스트)
    private String date; // "2025-12-25"
    private List<PlaceItem> places; // 아래에 내부 클래스로 정의

    // --- 매핑 설정 ---

    @DynamoDbPartitionKey // 이게 PK라는 뜻
    @DynamoDbAttribute("PK") // 실제 DB 컬럼 이름
    public String getPk() { return pk; }

    @DynamoDbSortKey // 이게 SK라는 뜻
    @DynamoDbAttribute("SK") // 실제 DB 컬럼 이름
    public String getSk() { return sk; }

    public void setSessionId(String sessionId) {
    }

    // --- 내부 클래스 (장소 정보) ---
    @Getter @Setter @DynamoDbBean
    public static class PlaceItem {
        private Long order; // 여행 목록 순서
        private String placeName; // 장소 이름
        private String time; // 방문 시간
        private String category; // 아이콘 결정용
        private Double lat; // 위도
        private Double lng; // 경도
        private String address; // 주소
        private String mapId; // 매장 식별자
    }
}