package com.jeju.ormicamp.model.dynamodb;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Getter
@Setter
@ToString
@DynamoDbBean // "이 클래스는 DynamoDB 테이블입니다"라고 알려주는 표시
public class MemberDynamoEntity {

    private String id;        // PK (파티션 키)
    private String name;      // 이름
    private String email;     // 이메일

    // 🚨 중요: AWS 콘솔에서 만든 파티션 키 이름('id')과 똑같은 변수의 Getter 위에 붙여야 합니다.
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}