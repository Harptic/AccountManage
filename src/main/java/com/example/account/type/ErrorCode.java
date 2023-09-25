package com.example.account.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("사용자가 없습니다."),
    MAX_ACCOUNT_PER_USER_10("사용자의 계좌는 10개를 넘을 수 없습니다."),
    ACCOUNT_NOT_FOUND("해당 계좌가 없습니다."),
    USER_ACCOUNT_UN_MATCHED("사용자와 계좌의 소유주가 다릅니다."),
    ACCOUNT_ALREADY_UNREGISTERED("이미 해지된 계좌입니다."),
    AMOUNT_EXCEED_BALANCE("거래 금액이 잔액보다 많습니다"),
    BALANCE_OVER_ZERO("잔액이 남아있는 계좌는 해지할 수 없습니다.")
    ;

    private final String description;
}
