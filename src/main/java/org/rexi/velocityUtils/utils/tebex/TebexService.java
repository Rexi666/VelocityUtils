package org.rexi.velocityUtils.utils.tebex;

import com.google.gson.*;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class TebexService {

    private static final String BASE_URL = "https://plugin.tebex.io";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final Logger logger;
    private final String secretKey;

    private final Object refreshLock = new Object();
    private volatile boolean refreshing = false;

    // Caché en memoria
    private volatile DonorStats cachedStats = null;
    private volatile long lastRefresh = 0;
    private final long cacheTtlMs;

    /**
     * @param secretKey   Tu clave secreta de Tebex (plugin API key)
     * @param cacheTtlMin Minutos entre refresco automático
     */
    public TebexService(Logger logger, String secretKey, int cacheTtlMin) {
        this.logger = logger;
        this.secretKey = secretKey;
        this.cacheTtlMs = cacheTtlMin * 60_000L;
    }

    // ------------------------------------------------------------------ //
    //  API                                                               //
    // ------------------------------------------------------------------ //

    /** Devuelve las estadísticas (de caché si no han expirado). */
    public CompletableFuture<DonorStats> getStats() {
        if (cachedStats != null && System.currentTimeMillis() - lastRefresh < cacheTtlMs) {
            return CompletableFuture.completedFuture(cachedStats);
        }
        return refresh();
    }

    /** Fuerza un refresco desde Tebex aunque la caché esté vigente. */
    public CompletableFuture<DonorStats> refresh() {
        synchronized (refreshLock) {
            if (refreshing) {
                logger.info("[VelocityUtils + Tebex] Refresh already in progress, skipping.");
                return cachedStats != null
                        ? CompletableFuture.completedFuture(cachedStats)
                        : CompletableFuture.failedFuture(new IllegalStateException("Refresh already in progress"));
            }
            refreshing = true;
        }

        return fetchAllPayments()
                .thenApply(payments -> {
                    DonorStats stats = aggregate(payments);
                    logger.info("[VelocityUtils + Tebex] Stats updated ({} purchases processed)", payments.size());
                    return stats;
                })
                .whenComplete((stats, err) -> {
                    synchronized (refreshLock) { refreshing = false; }
                    if (err != null) {
                        logger.error("[VelocityUtils + Tebex] Error obtaining purchases: {}", err.getMessage());
                    } else {
                        cachedStats = stats;
                        lastRefresh = System.currentTimeMillis();
                    }
                });
    }

    private CompletableFuture<List<JsonObject>> fetchAllPayments() {
        return apiGet("/payments?limit=100")
                .thenApply(body -> {
                    JsonArray array = gson.fromJson(body, JsonArray.class);
                    List<JsonObject> list = new ArrayList<>();
                    array.forEach(el -> list.add(el.getAsJsonObject()));
                    logger.info("[VelocityUtils + Tebex] Fetched {} purchases.", list.size());
                    return list;
                });
    }

    // ------------------------------------------------------------------ //
    //  Aggregation                                                       //
    // ------------------------------------------------------------------ //

    private DonorStats aggregate(List<JsonObject> payments) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth thisMonth = YearMonth.from(today);
        // Monday of actual week
        LocalDate weekStart = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);

        Map<String, double[]> allTime    = new LinkedHashMap<>();
        Map<String, double[]> monthly    = new LinkedHashMap<>();
        Map<String, double[]> weekly     = new LinkedHashMap<>();
        Map<String, double[]> daily      = new LinkedHashMap<>();
        Map<String, String>   names      = new LinkedHashMap<>();
        Map<String, String>   currencies = new LinkedHashMap<>();

        double revenueAllTime  = 0;
        double revenueThisMonth = 0;
        double revenueThisWeek = 0;
        double revenueToday    = 0;
        String mainCurrency = "USD";

        for (JsonObject p : payments) {
            JsonObject player = p.getAsJsonObject("player");
            String name   = player.get("name").getAsString();
            String uuid   = player.has("uuid") && !player.get("uuid").isJsonNull()
                    ? player.get("uuid").getAsString() : name;
            double amount   = p.get("amount").getAsDouble();
            if (amount <= 0) continue;
            String currency = p.getAsJsonObject("currency").get("iso_4217").getAsString();
            LocalDate date = OffsetDateTime.parse(p.get("date").getAsString(), DATE_FMT).toLocalDate();

            names.put(uuid, name);
            currencies.put(uuid, currency);
            mainCurrency = currency;
            revenueAllTime += amount;

            allTime.computeIfAbsent(uuid, k -> new double[]{0})[0] += amount;

            if (YearMonth.from(date).equals(thisMonth)) {
                monthly.computeIfAbsent(uuid, k -> new double[]{0})[0] += amount;
                revenueThisMonth += amount;
            }
            if (!date.isBefore(weekStart)) {
                weekly.computeIfAbsent(uuid, k -> new double[]{0})[0] += amount;
                revenueThisWeek += amount;
            }
            if (date.equals(today)) {
                daily.computeIfAbsent(uuid, k -> new double[]{0})[0] += amount;
                revenueToday += amount;
            }
        }

        List<TopDonor> topAllTime  = buildTop(allTime,  names, currencies, 1);
        List<TopDonor> topMonthly  = buildTop(monthly,  names, currencies, 1);
        List<TopDonor> topWeekly   = buildTop(weekly,   names, currencies, 1);
        List<TopDonor> topDaily    = buildTop(daily,    names, currencies, 1);

        return new DonorStats(
                topAllTime,  topMonthly,  topWeekly,  topDaily,
                topAllTime.isEmpty()  ? null : topAllTime.get(0),
                topMonthly.isEmpty()  ? null : topMonthly.get(0),
                topWeekly.isEmpty()   ? null : topWeekly.get(0),
                topDaily.isEmpty()    ? null : topDaily.get(0),
                Math.round(revenueAllTime  * 100.0) / 100.0,
                Math.round(revenueThisMonth * 100.0) / 100.0,
                Math.round(revenueThisWeek * 100.0) / 100.0,
                Math.round(revenueToday * 100.0) / 100.0,
                mainCurrency
        );
    }

    private List<TopDonor> buildTop(Map<String, double[]> totals,
                                    Map<String, String> names,
                                    Map<String, String> currencies,
                                    int limit) {
        return totals.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .limit(limit)
                .map(e -> new TopDonor(
                        names.getOrDefault(e.getKey(), e.getKey()),
                        e.getKey(),
                        Math.round(e.getValue()[0] * 100.0) / 100.0,
                        currencies.getOrDefault(e.getKey(), "USD")
                ))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ //
    //  HTTP helper                                                         //
    // ------------------------------------------------------------------ //

    private CompletableFuture<String> apiGet(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("X-Tebex-Secret", secretKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Tebex API " + response.statusCode()
                                + " en " + path + ": " + response.body());
                    }
                    return response.body();
                });
    }
}
