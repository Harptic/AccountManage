package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.USE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepositoy;



    @Transactional
    public TransactionDto useBalance(Long userID, String accountNumber, Long amount){
        AccountUser user = accountUserRepository.findById(userID)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        Account account = accountRepositoy.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        validateBalance(user, account, amount);

        account.useBalance(amount);

        Transaction transaction = transactionRepository.save(
                Transaction.builder()
                        .transactionType(USE)
                        .transactionResultType(S)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionID(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );

        return TransactionDto.fromEntity(transaction);
        //Long accountBalance = account.getBalance();
        //account.setBalance(accountBalance - amount);
    }

    /**
     * 1. 사용자가 없는 경우
     * 2. 사용자 아이디와 계좌 소유주가 다른 경우
     * 3. 계좌가 이미 혜지상태인 경우
     * 4. 거래금액이 잔액보다 큰 경우
     * 5. 거래금액이 너무 작거나 큰 경우
     *
     */

    private void validateBalance(AccountUser user, Account account, Long amount) {
        if(!Objects.equals(user.getID(), account.getAccountUser().getID())){
            throw  new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCHED);
        }
        if(account.getAccountStatus() != AccountStatus.IN_USE){
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if(account.getBalance() < amount){
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepositoy.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        transactionRepository.save(
                Transaction.builder()
                        .transactionType(USE)
                        .transactionResultType(F)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionID(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );

    }
}
