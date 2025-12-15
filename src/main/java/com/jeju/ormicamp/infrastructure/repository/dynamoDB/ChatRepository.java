package com.jeju.ormicamp.infrastructure.repository.dynamoDB;

import com.jeju.ormicamp.model.dynamodb.ChatEntity; // 아까 만든 Entity import
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import java.util.ArrayList;

@Repository
public class ChatRepository {

    private final DynamoDbTable<ChatEntity> table;

    public ChatRepository(DynamoDbEnhancedClient enhancedClient) {
        // 테이블 이름 "JejuTravel" (AWS 콘솔에서 만든 이름과 같아야 함)
        this.table = enhancedClient.table("fall-in-jeju", TableSchema.fromBean(ChatEntity.class));
    }

    // 1. 저장 (뭐든지 저장 가능)
    public void save(ChatEntity item) {
        table.putItem(item);
    }

    // 2. 내 여행 목록 조회 (PK = USER#... 로 조회)
    public List<ChatEntity> findSessionsByUserId(String userId) {
        // 조건: PK가 "USER#userId" 인 것
        QueryConditional queryConditional = QueryConditional.keyEqualTo(k ->
                k.partitionValue("USER#" + userId)
        );

        // 결과 반환 (Iterator를 List로 변환)
        List<ChatEntity> results = new ArrayList<>();
        table.query(queryConditional).items().forEach(results::add);
        return results;
    }

    // 3. 특정 세션의 모든 데이터(채팅+플래너) 조회
    public List<ChatEntity> findAllInSession(String sessionId) {
        // 조건: PK가 "SESSION#sessionId" 인 것
        QueryConditional queryConditional = QueryConditional.keyEqualTo(k ->
                k.partitionValue("SESSION#" + sessionId)
        );

        List<ChatEntity> results = new ArrayList<>();
        table.query(queryConditional).items().forEach(results::add);
        return results;
    }
}