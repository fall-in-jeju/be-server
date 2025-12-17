package com.jeju.ormicamp.infrastructure.repository.dynamoDB;

import com.jeju.ormicamp.common.config.bedrock.AwsProperties;
import com.jeju.ormicamp.common.exception.CustomException;
import com.jeju.ormicamp.common.exception.ErrorCode;
import com.jeju.ormicamp.model.domain.ChatEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;



@Repository
@RequiredArgsConstructor
public class ChatDynamoRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AwsProperties awsProperties;


    private DynamoDbTable<ChatEntity> table() {
        return enhancedClient.table(
                awsProperties.getDynamoTableName(),
                TableSchema.fromBean(ChatEntity.class)
        );
    }

    /**
     * =====================
     * 1. 저장 (META / CHAT 공용)
     * =====================
     */
    public void save(ChatEntity entity) {
        table().putItem(entity);
    }

    /**
     * =====================
     * 2. META 조회
     * =====================
     * Service 코드에서:
     * ChatEntity meta = chatRepository.findMeta(conversationId);
     */
    public ChatEntity findMeta(String conversationId) {

        Key key = Key.builder()
                .partitionValue("SESSION#" + conversationId)
                .sortValue("META")
                .build();

        ChatEntity meta = table().getItem(r -> r.key(key));

        if (meta == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        return meta;
    }
}
