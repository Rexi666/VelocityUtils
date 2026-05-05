package org.rexi.velocityUtils.utils.tebex;

import java.util.List;

public record DonorStats(
        // Top N listas
        List<TopDonor> topAllTime,
        List<TopDonor> topThisMonth,
        List<TopDonor> topThisWeek,
        List<TopDonor> topToday,
        // Líderes
        TopDonor allTimeLeader,
        TopDonor monthlyLeader,
        TopDonor weeklyLeader,
        TopDonor dailyLeader,
        // Ingresos
        double revenueAllTime,
        double revenueThisMonth,
        double revenueThisWeek,
        double revenueToday,
        String currency
) {}
