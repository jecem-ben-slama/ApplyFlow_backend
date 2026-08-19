package com.applyflow.tracker_api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountDeletionScheduler {

    private final AccountDeletionService accountDeletionService;

    @Scheduled(cron = "0 0 3 * * *") // daily at 3am server time
    public void purgeExpiredAccounts() {
        accountDeletionService.purgeExpiredAccounts();
    }
}