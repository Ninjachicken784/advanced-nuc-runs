package com.ninjachicken784.advancednucruns.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import com.ninjachicken784.advancednucruns.AdvancedNucRuns;
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
	private static final long REFRESH_MILLIS = 5 * 60_000L;

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	private static final AtomicBoolean BUSY = new AtomicBoolean();

	private static volatile String status = "Prices not loaded yet";
	private static volatile long lastAttempt = Long.MIN_VALUE / 2;

	private PriceService() { }

	static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (AdvancedNucRunsClient.nowMillis() - lastAttempt >= REFRESH_MILLIS) {
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
		status = problems.length() == 0 ? "Prices updated" : "Prices:" + problems;
		BUSY.set(false);
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
