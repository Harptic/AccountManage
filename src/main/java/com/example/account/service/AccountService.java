package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.*;
import static com.example.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    // 해당 사용자가 있는지 확인
    // 계좌번호 생성하고 계좌를 저장 후, 그 정보를 넘김
    @Transactional
    public AccountDto createAccount(Long userID, Long initialBalance) {

        //Exeption을 새로 정의해서 없으면 throw하도록 함.
        AccountUser accountUser = accountUserRepository.findById(userID)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
                .orElse("1000000000");

        Account savedAccount = accountRepository.save(
                Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .accountNumber(newAccountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build()
        );

        return AccountDto.fromEntity(savedAccount);
    }

    @Transactional
    public Account getAccount(Long id) {
        if(id < 0){
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

    // 사용자 또는 계좌가 없을 때, 아이디와 계좌 소유주가 다를 때, 계좌가 이미 해지 상태일 때,
    // 잔액이 있는 경우에는 delete할 수 없게 한다.
    @Transactional
    public AccountDto deleteAccount(Long userID, String accountNumber) {
        AccountUser accountUser = accountUserRepository.findById(userID)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnregisteredAt(LocalDateTime.now());

        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        //사용자와 계좌의 소유주과 다른 케이스
        if(!Objects.equals(accountUser.getID(), account.getAccountUser().getID())){
            throw new AccountException(USER_ACCOUNT_UN_MATCHED);
        }
        //사용자의 계좌가 이미 해지된 케이스
        if(account.getAccountStatus() == AccountStatus.UNREGISTERED){
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }
        //계좌에 잔액이 남아있는 케이스
        if(account.getBalance() > 0){
            throw new AccountException(BALANCE_OVER_ZERO);
        }
    }

    @Transactional
    public List<AccountDto> getAccountsByUserID(Long userID) {
        AccountUser accountUser = accountUserRepository.findById(userID)
                .orElseThrow(() -> new AccountException((USER_NOT_FOUND)));
        List<Account> accounts = accountRepository
                .findByAccountUser(accountUser);

        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
    }
}
