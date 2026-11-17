package com.ninjachicken784.advancednucruns.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import com.ninjachicken784.advancednucruns.AdvancedNucRuns;
import com.ninjachicken784.advancednucruns.NucleusItems;
import com.ninjachicken784.advancednucruns.PriceBook;
import com.ninjachicken784.advancednucruns.PriceFeed;

/**
 * Downloads the prices in the background: Bazaar prices and item names from the Hypixel API and
 * lowest BIN prices for the Auction House. Runs at start and then every few minutes. If a
 * download fails the old prices are kept.
 */
final class PriceService {
	private static final String BAZAAR = "https://api.hypixel.net/v2/skyblock/bazaar";
	private static final String ITEMS = "https://api.hypixel.net/v2/resources/skyblock/items";
	private static final String[] BIN_URLS = {
		"https://lbin.neu.io/lowestbin.json",
		"https://moulberry.codes/lowestbin.json"
	};
	private static final String AUCTIONS = "https://api.hypixel.net/v2/skyblock/auctions?page=";
	private static final long REFRESH_MILLIS = 5 * 60_000L;
	/** Scanning every auction is heavy, so it is only done for drops that still have no price, and not often. */
	private static final long SCAN_EVERY_MILLIS = 15 * 60_000L;
	/** A drop that has never had a price is looked up again sooner. */
	private static final long RETRY_SCAN_EVERY_MILLIS = 3 * 60_000L;
	private static final int MAX_AUCTION_PAGES = 150;

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	private static final AtomicBoolean BUSY = new AtomicBoolean();

	private static volatile String status = "Prices not loaded yet";
	private static volatile long lastAttempt = Long.MIN_VALUE / 2;
	private static volatile long lastScan = Long.MIN_VALUE / 2;

	private PriceService() { }

	static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Nothing is downloaded while the mod or the tracker is switched off.
			if (HudVisibility.trackerOn() && AdvancedNucRunsClient.nowMillis() - lastAttempt >= REFRESH_MILLIS) {
				refresh();
			}
		});
	}

	/** What the price download is doing, for the tracker screen. */
	static String status() {
		return status;
	}

	/** Starts a download now, unless one is already running. */
	static void refresh() {
		if (!BUSY.compareAndSet(false, true)) {
			return;
		}
		lastAttempt = AdvancedNucRunsClient.nowMillis();
		status = "Updating prices...";
		Thread thread = new Thread(PriceService::download, "advanced-nuc-runs-prices");
		thread.setDaemon(true);
		thread.start();
	}

	private static void download() {
		try {
			downloadAll();
		} catch (RuntimeException e) {
			status = "Prices: update failed";
			AdvancedNucRuns.LOGGER.warn("The price update failed", e);
		} finally {
			// Always allow the next update, whatever went wrong.
			BUSY.set(false);
		}
	}

	private static void downloadAll() {
		StringBuilder problems = new StringBuilder();
		try {
			AdvancedNucRunsClient.PRICES.setBazaar(PriceFeed.parseBazaar(get(BAZAAR)));
		} catch (Exception e) {
			problems.append(" Bazaar failed.");
			AdvancedNucRuns.LOGGER.warn("Could not load the Bazaar prices", e);
		}
		boolean bins = false;
		for (String url : BIN_URLS) {
			try {
				AdvancedNucRunsClient.PRICES.setBins(PriceFeed.parseBins(get(url)));
				bins = true;
				break;
			} catch (Exception e) {
				AdvancedNucRuns.LOGGER.warn("Could not load lowest BIN prices from {}", url, e);
			}
		}
		if (!bins) {
			problems.append(" Lowest BIN failed.");
		}
		try {
			AdvancedNucRunsClient.PRICES.setIdsByName(PriceFeed.parseNames(get(ITEMS)));
		} catch (Exception e) {
			problems.append(" Item names failed.");
			AdvancedNucRuns.LOGGER.warn("Could not load the item names", e);
		}

		// Drops that no list has a price for (a wrong item id, or the BIN list is down) are looked up
		// directly in the Auction House by their name. What a scan finds is added to what earlier scans
		// found, so a price stays until a newer one replaces it.
		boolean scanned = false;
		try {
			Set<String> missing = unlistedNames();
			if (!missing.isEmpty() && AdvancedNucRunsClient.PRICES.hasBazaar()
				&& AdvancedNucRunsClient.nowMillis() - lastScan >= (unpricedCount() > 0 ? RETRY_SCAN_EVERY_MILLIS : SCAN_EVERY_MILLIS)) {
				status = "Scanning the Auction House...";
				lastScan = AdvancedNucRunsClient.nowMillis();
				AdvancedNucRunsClient.PRICES.addNameBins(scanAuctions(missing));
				scanned = true;
			}
		} catch (Exception e) {
			problems.append(" Auction scan failed.");
			AdvancedNucRuns.LOGGER.warn("Could not scan the Auction House", e);
		}

		String base = problems.length() == 0 ? "Prices updated" : "Prices:" + problems;
		int stillMissing = unpricedCount();
		status = base + (scanned ? " (auction scan)" : "") + (stillMissing > 0 ? ", " + stillMissing + " unpriced" : "");
		// Keep the new prices on disk, so they are there right away the next time the game starts.
		TrackerStore.save();
	}

	/** Normalized names of the drops that no price list has a price for, so they must be looked up in the Auction House. */
	private static Set<String> unlistedNames() {
		PriceBook prices = AdvancedNucRunsClient.PRICES;
		PriceBook.Mode mode = AdvancedNucRunsClient.TRACKER.priceMode();
		Set<String> missing = new HashSet<>();
		for (NucleusItems.Item item : NucleusItems.ALL) {
			if (Double.isNaN(prices.listedPrice(item, mode))) {
				missing.add(NucleusItems.normalize(item.name()));
			}
		}
		for (String name : AdvancedNucRunsClient.TRACKER.customDrops().values()) {
			if (Double.isNaN(prices.listedPriceByName(name, mode))) {
				missing.add(NucleusItems.normalize(name));
			}
		}
		return missing;
	}

	/** How many drops have no price at all right now, not even a remembered one. */
	private static int unpricedCount() {
		PriceBook prices = AdvancedNucRunsClient.PRICES;
		PriceBook.Mode mode = AdvancedNucRunsClient.TRACKER.priceMode();
		int count = 0;
		for (NucleusItems.Item item : NucleusItems.ALL) {
			if (Double.isNaN(prices.price(item, mode))) {
				count++;
			}
		}
		for (String name : AdvancedNucRunsClient.TRACKER.customDrops().values()) {
			if (Double.isNaN(prices.priceByName(name, mode))) {
				count++;
			}
		}
		return count;
	}

	/** The lowest BIN of each wanted name, found by reading every page of the Auction House. */
	private static Map<String, Double> scanAuctions(Set<String> wanted) throws Exception {
		PriceFeed.AuctionPage first = PriceFeed.parseAuctionPage(get(AUCTIONS + 0), wanted);
		Map<String, Double> lowest = new HashMap<>(first.lowestByName());

		int pages = Math.min(first.totalPages(), MAX_AUCTION_PAGES);
		ExecutorService pool = Executors.newFixedThreadPool(4, runnable -> {
			Thread thread = new Thread(runnable, "advanced-nuc-runs-auctions");
			thread.setDaemon(true);
			return thread;
		});
		try {
			List<Future<PriceFeed.AuctionPage>> futures = new ArrayList<>();
			for (int page = 1; page < pages; page++) {
				int number = page;
				futures.add(pool.submit(() -> PriceFeed.parseAuctionPage(get(AUCTIONS + number), wanted)));
			}
			for (Future<PriceFeed.AuctionPage> future : futures) {
				try {
					future.get().lowestByName().forEach((name, price) -> lowest.merge(name, price, Math::min));
				} catch (Exception e) {
					// One page failing only means a few auctions are missed.
					AdvancedNucRuns.LOGGER.debug("An auction page failed", e);
				}
			}
		} finally {
			pool.shutdownNow();
		}
		return lowest;
	}

	private static String get(String url) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(30))
			.header("User-Agent", "advanced-nuc-runs")
			.GET()
			.build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
		}
		return response.body();
	}
}
