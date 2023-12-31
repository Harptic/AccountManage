package com.example.account.service;

import com.example.account.AccountApplication;
import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.repository.AccountRepository;
import com.example.account.type.ErrorCode;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("계좌 생성 성공")
    void createAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder()
                .ID(12L)
                .name("test")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012")
                        .build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 100L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserID());
        assertEquals("1000000013", accountDto.getAccountNumber());

    }

    @Test
    @DisplayName("계좌 해지 성공")
    void deleteAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder()
                .ID(12L)
                .name("test")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserID());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());

    }

    @Test
    @DisplayName("첫 계좌 생성")
    void createFirstAccount() {
        //given
        AccountUser user = AccountUser.builder()
                .ID(15L)
                .name("test")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000000").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 100L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserID());
        assertEquals("1000000000", accountDto.getAccountNumber());

    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccountFailUserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 100L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void deleteAccountFailUserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccountFailAccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .ID(12L)
                .name("test")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 계좌 해지 실패")
    void deleteAccountFailUserUnMatch() {
        //given
        AccountUser test1 = AccountUser.builder()
                .ID(12L)
                .name("test1")
                .build();
        AccountUser test2 = AccountUser.builder()
                .ID(13L)
                .name("test2")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(test1));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(test2)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCHED, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 잔액 있음 - 계좌 해지 실패")
    void deleteAccountFailBalanceOverZero() {
        //given
        AccountUser test = AccountUser.builder()
                .ID(12L)
                .name("test")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(test));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(test)
                        .balance(100L)
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.BALANCE_OVER_ZERO, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 해지된 계좌 - 계좌 해지 실패")
    void deleteAccountFailAlreadyUnRegistered() {
        //given
        AccountUser test = AccountUser.builder()
                .ID(12L)
                .name("test")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(test));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(test)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("유저 당 최대 계자는 10개")
    void createAccountFailMaxAccountIs10() {
        //given
        AccountUser user = AccountUser.builder()
                .ID(18L)
                .name("test").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 100L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 조회 성공")
    void successGetAccountsByUserID() {
        //given
        AccountUser user = AccountUser.builder()
                .ID(18L)
                .name("test").build();
        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountNumber("1111111111")
                        .accountUser(user)
                        .balance(1000L)
                        .build(),
                Account.builder()
                        .accountNumber("2222222222")
                        .accountUser(user)
                        .balance(1000L)
                        .build(),
                Account.builder()
                        .accountNumber("3333333333")
                        .accountUser(user)
                        .balance(1000L)
                        .build()
        );
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);
        //when
        List<AccountDto> accountsDtos = accountService.getAccountsByUserID(1L);

        //then
        assertEquals(3, accountsDtos.size());
        assertEquals("1111111111", accountsDtos.get(0).getAccountNumber());
        assertEquals(1000, accountsDtos.get(0).getBalance());
        assertEquals("2222222222", accountsDtos.get(0).getAccountNumber());
        assertEquals(1000, accountsDtos.get(0).getBalance());
        assertEquals("3333333333", accountsDtos.get(0).getAccountNumber());
        assertEquals(1000, accountsDtos.get(0).getBalance());

    }

    @Test
    void failedToGetAccounts() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserID(1L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}