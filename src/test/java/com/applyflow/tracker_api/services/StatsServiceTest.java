package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.StatsSummaryDto;
import com.applyflow.tracker_api.repositories.ApplicationEventRepository;
import com.applyflow.tracker_api.repositories.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationEventRepository applicationEventRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void getSummaryIncludesCurrentAndPreviousPeriodComparison() {
        Long userId = 42L;
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59);

        when(applicationRepository.findApplicationIdsByUserIdAndFilters(
                eq(userId), any(), any(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(java.util.List.of(1L, 2L, 3L));
        when(applicationRepository.countByUserIdAndFilters(eq(userId), any(), any(), isNull(), isNull(), isNull(),
                isNull()))
                .thenReturn(3L);
        when(applicationRepository.countByUserIdAndStatusInFilters(eq(userId), anyList(), any(), any(), isNull(),
                isNull(), isNull(), isNull()))
                .thenReturn(2L);
        when(applicationEventRepository.countDistinctApplicationsByStatusAndApplicationIds(
                eq(userId), eq("SENT"), anyList(), any(), any()))
                .thenReturn(3L);
        when(applicationEventRepository.countDistinctApplicationsByStatusAndApplicationIds(
                eq(userId), eq("RESPONDED"), anyList(), any(), any()))
                .thenReturn(2L);
        when(applicationEventRepository.countDistinctApplicationsByStatusAndApplicationIds(
                eq(userId), eq("VIEWED"), anyList(), any(), any()))
                .thenReturn(2L);
        when(applicationEventRepository.countDistinctApplicationsByStatusAndApplicationIds(
                eq(userId), eq("INTERVIEWING"), anyList(), any(), any()))
                .thenReturn(1L);
        when(applicationEventRepository.countDistinctApplicationsByStatusAndApplicationIds(
                eq(userId), eq("OFFER"), anyList(), any(), any()))
                .thenReturn(1L);
        when(applicationEventRepository.findEarliestEventByApplicationIds(
                eq(userId), eq("SENT"), anyList(), any(), any()))
                .thenReturn(java.util.List.of());
        when(applicationEventRepository.findEarliestEventByApplicationIds(
                eq(userId), eq("RESPONDED"), anyList(), any(), any()))
                .thenReturn(java.util.List.of());

        StatsSummaryDto dto = statsService.getSummary(userId, from, to, null, null, null, null);

        assertEquals(3L, dto.getTotalApplications());
        assertNotNull(dto.getCurrentPeriod());
        assertNotNull(dto.getPreviousPeriod());
        assertEquals(3L, dto.getCurrentPeriod().getTotalApplications());
        assertEquals(3L, dto.getPreviousPeriod().getTotalApplications());
    }
}
