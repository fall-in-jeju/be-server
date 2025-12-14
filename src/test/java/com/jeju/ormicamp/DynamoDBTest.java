package com.jeju.ormicamp;

import com.jeju.ormicamp.infrastructure.repository.dynamoDB.MemberDynamoRepository;
import com.jeju.ormicamp.model.dynamodb.MemberDynamoEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DynamoDBTest {

    @Autowired
    private MemberDynamoRepository repository;

    @Test
    void dynamoDB_save_and_find() {
        System.out.println("========== [테스트 시작] ==========");

        // 1. 저장할 데이터 만들기
        String myId = "test_user_001";
        MemberDynamoEntity member = new MemberDynamoEntity();
        member.setId(myId);
        member.setName("김코딩");
        member.setEmail("hello@example.com");

        // 2. AWS DynamoDB에 전송!
        repository.save(member);
        System.out.println(">> 데이터 전송 완료!");

        // 3. 잘 들어갔나 다시 꺼내오기
        MemberDynamoEntity foundMember = repository.findById(myId);

        if (foundMember != null) {
            System.out.println(">> 조회 성공! 이름: " + foundMember.getName());
            System.out.println(">> ID: " + foundMember.getId());
        } else {
            System.out.println(">> ㅠㅠ 조회 실패...");
        }

        System.out.println("========== [테스트 종료] ==========");
    }
}