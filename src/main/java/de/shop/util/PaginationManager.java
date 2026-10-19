package de.shop.util;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PaginationManager {

    /** حداکثر تعداد صفحات مجاز. */
    public static final int MAX_PAGES = 20;

    /** تعداد آیتم‌های نمایش‌داده‌شده در هر صفحه. */
    public static final int ITEMS_PER_PAGE = 45;

    /** صفحه فعلی هر بازیکن. */
    private final Map<UUID, Integer> currentPage = new HashMap<>();

    /**
     * دریافت شماره صفحه فعلی بازیکن (از 0 شروع می‌شود).
     */
    public int getPage(Player player) {
        return currentPage.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * تنظیم شماره صفحه بازیکن با محدودسازی بین 0 تا MAX_PAGES - 1.
     */
public void setPage(Player player, int page) {
    int clamped = Math.max(0, Math.min(page, MAX_PAGES - 1));
    currentPage.put(player.getUniqueId(), clamped);
}

    /**
     * رفتن به صفحه بعد (اگر وجود داشته باشد).
     */
    public void nextPage(Player player) {
        setPage(player, getPage(player) + 1);
    }

    /**
     * برگشتن به صفحه قبل (اگر وجود داشته باشد).
     */
    public void previousPage(Player player) {
        setPage(player, getPage(player) - 1);
    }

    /**
     * پاک کردن وضعیت صفحه‌بندی یک بازیکن.
     */
    public void reset(Player player) {
        currentPage.remove(player.getUniqueId());
    }

    /**
     * پاک کردن همه وضعیت‌ها (هنگام disable پلاگین).
     */
    public void resetAll() {
        currentPage.clear();
    }

    /**
     * برش دادن لیست کل آیتم‌ها به لیست آیتم‌های یک صفحه خاص.
     */
    public <T> List<T> getPageItems(List<T> allItems, int page) {
        if (allItems == null || allItems.isEmpty()) return Collections.emptyList();

        int start = page * ITEMS_PER_PAGE;
        if (start >= allItems.size()) return Collections.emptyList();

        int end = Math.min(start + ITEMS_PER_PAGE, allItems.size());
        return allItems.subList(start, end);
    }

    /**
     * محاسبه تعداد کل صفحات بر اساس تعداد آیتم‌ها.
     */
    public int getTotalPages(int totalItems) {
        if (totalItems <= 0) return 1;
        int pages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        return Math.min(pages, MAX_PAGES);
    }

    /**
     * آیا صفحه بعدی وجود دارد؟
     */
    public boolean hasNextPage(Player player, List<?> allItems) {
        int total = getTotalPages(allItems.size());
        return getPage(player) < total - 1;
    }

    /**
     * آیا صفحه قبلی وجود دارد؟
     */
    public boolean hasPreviousPage(Player player) {
        return getPage(player) > 0;
    }
}