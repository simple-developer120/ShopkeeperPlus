package de.shop.util;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PaginationManager {

    public static final int MAX_PAGES = 20;

    public static final int ITEMS_PER_PAGE = 45;

    private final Map<UUID, Integer> currentPage = new HashMap<>();

    public int getPage(Player player) {
        return currentPage.getOrDefault(player.getUniqueId(), 0);
    }

public void setPage(Player player, int page) {
    int clamped = Math.max(0, Math.min(page, MAX_PAGES - 1));
    currentPage.put(player.getUniqueId(), clamped);
}

    public void nextPage(Player player) {
        setPage(player, getPage(player) + 1);
    }

    public void previousPage(Player player) {
        setPage(player, getPage(player) - 1);
    }

    public void reset(Player player) {
        currentPage.remove(player.getUniqueId());
    }

    public void resetAll() {
        currentPage.clear();
    }

    public <T> List<T> getPageItems(List<T> allItems, int page) {
        if (allItems == null || allItems.isEmpty()) return Collections.emptyList();

        int start = page * ITEMS_PER_PAGE;
        if (start >= allItems.size()) return Collections.emptyList();

        int end = Math.min(start + ITEMS_PER_PAGE, allItems.size());
        return allItems.subList(start, end);
    }

    public int getTotalPages(int totalItems) {
        if (totalItems <= 0) return 1;
        int pages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        return Math.min(pages, MAX_PAGES);
    }

    public boolean hasNextPage(Player player, List<?> allItems) {
        int total = getTotalPages(allItems.size());
        return getPage(player) < total - 1;
    }

    public boolean hasPreviousPage(Player player) {
        return getPage(player) > 0;
    }
}
