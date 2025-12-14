package com.jeju.ormicamp.model.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDateTime createDate;

    private LocalDateTime updateDate;


    /**
     * 수정한 날짜를 저장
     * 단, 수정하지 않은 부분은 null 값으로 오기 때문에 저장 x
     * @param startDate 시작일
     * @param endDate 종료일
     */
    public void updateDate(LocalDate startDate, LocalDate endDate) {

        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }

    }


}
