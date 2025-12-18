package com.jeju.ormicamp.model.domain;

import com.jeju.ormicamp.model.code.ChatRole;
import com.jeju.ormicamp.model.code.ChatType;
import com.jeju.ormicamp.model.code.TravelInfoSnapshot;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDbBean
public class ChatEntity {

    private String pk;
    private String sk;

    private String conversationId;
    private String timestamp;


    private ChatType type;
    private ChatRole role;

    private String prompt;

    private String chatTitle;

    private TravelInfoSnapshot travelInfo;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK") // 실제 DB 컬럼 이름
    public String getPk() {
        return "SESSION#" + conversationId;
    }

    @DynamoDbSortKey // 이게 SK라는 뜻
    @DynamoDbAttribute("SK") // 실제 DB 컬럼 이름
    public String getSk() {
        if (type == ChatType.PLAN_META) {
            return "META";
        }
        return type.name() + "#" + timestamp;
    }
}