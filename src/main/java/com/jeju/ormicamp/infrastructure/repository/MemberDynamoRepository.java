package com.jeju.ormicamp.infrastructure.repository;

import com.jeju.ormicamp.model.dynamodb.MemberDynamoEntity; // 아까 만든 Entity import
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class MemberDynamoRepository {

    private final DynamoDbTable<MemberDynamoEntity> table;

    // 생성자 주입: 아까 Config에서 만든 enhancedClient를 가져옵니다.
    public MemberDynamoRepository(DynamoDbEnhancedClient enhancedClient) {
        // "Member"라는 테이블과 MemberDynamoEntity 클래스를 연결(매핑)합니다.
        this.table = enhancedClient.table("four-in-jeju", TableSchema.fromBean(MemberDynamoEntity.class));
    }

    // 1. 저장 (Save)
    public void save(MemberDynamoEntity member) {
        table.putItem(member);
    }

    // 2. 조회 (Find)
    public MemberDynamoEntity findById(String id) {
        // PK(파티션 키)를 사용해 단건 조회
        return table.getItem(Key.builder().partitionValue(id).build());
    }
}