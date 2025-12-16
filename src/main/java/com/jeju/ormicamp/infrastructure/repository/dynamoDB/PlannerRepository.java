package com.jeju.ormicamp.infrastructure.repository.dynamoDB;

import com.jeju.ormicamp.model.dynamodb.ChatEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PlannerRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    // 테이블 객체 생성
    private DynamoDbTable<ChatEntity> getTable() {
        return dynamoDbEnhancedClient.table("fall-in-jeju", TableSchema.fromBean(ChatEntity.class));
    }

    // 1. 플래너 저장 (생성 & 수정 통합 - Upsert)
    public void savePlan(ChatEntity entity) {
        log.info("Saving Plan -> PK: {}, SK: {}", entity.getPk(), entity.getSk());
        getTable().putItem(entity); // PK, SK가 같으면 덮어씌움
    }

    // 2. 특정 날짜의 플래너 조회 (단건 조회)
    public ChatEntity findPlanBySessionAndDate(String sessionId, String date) {
        Key key = Key.builder()
                .partitionValue("SESSION#" + sessionId)
                .sortValue("PLAN#" + date)
                .build();

        return getTable().getItem(key);
    }

    // 3. 특정 세션의 모든 플래너 조회 (전체 일정 조회)
    // PK는 SESSION#{id}이고, SK가 PLAN#으로 시작하는 것만 가져옴
    public List<ChatEntity> findAllPlansBySession(String sessionId) {
        // QueryConditional 등을 사용하여 SK가 "PLAN#"으로 시작하는 조건 추가 가능
        // 여기서는 간단하게 해당 파티션의 모든 아이템을 가져온 뒤 필터링하는 방식 예시 (데이터 양에 따라 쿼리 조건 최적화 필요)

        return getTable().query(r -> r.queryConditional(
                        software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(
                                Key.builder().partitionValue("SESSION#" + sessionId).build()
                        )
                )).items().stream()
                .filter(item -> "PLAN".equals(item.getType())) // 타입이 PLAN인 것만 필터링
                .collect(Collectors.toList());
    }

    // 4. 플래너 삭제
    public void deletePlan(String sessionId, String date) {
        Key key = Key.builder()
                .partitionValue("SESSION#" + sessionId)
                .sortValue("PLAN#" + date)
                .build();
        getTable().deleteItem(key);
    }
}