package com.ninjachicken784.advancednucruns;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The latest prices from the game: Bazaar prices and lowest BIN prices from the Auction House.
 * Filled in by the price feed in the background. Contains no Minecraft code so it can be tested
 * on its own.
 */
public final class PriceBook {
	/** Which price a Bazaar item is worth when you sell it. */
	public enum Mode {
		/** The lowest sell offer, which is what you get if you list it and wait. */
		SELL_OFFER,
		/** The highest buy order, which is what you get if you sell it straight away. */
		INSTANT_SELL
	}

	/**
	 * Both Bazaar prices of one item. The sell offer is always the higher one, because the lowest
	 * sell offer is above the highest buy order.
	 */
	public record Bazaar(double sellOffer, double instantSell) { }

	private volatile Map<String, Bazaar> bazaar = Map.of();
	private volatile Map<String, Double> bins = Map.of();
	private volatile Map<String, String> idsByName = Map.of();
	/** Lowest BINs found by scanning the Auction House, by normalized item name. Only ever added to. */
	private volatile Map<String, Double> nameBins = Map.of();
	/**
	 * The last real price found for every item, by mode and normalized name. When a download
	 * fails or an item is briefly missing from a list, this is shown instead of a "?".
	 */
	private final Map<String, Double> lastGood = new ConcurrentHashMap<>();

	/** Builds a Bazaar entry from the two prices in either order. */
	public static Bazaar bazaarOf(double one, double other) {
		return new Bazaar(Math.max(one, other), Math.min(one, other));
	}

	public void setBazaar(Map<String, Bazaar> prices) {
		bazaar = Map.copyOf(prices);
	}

	public void setBins(Map<String, Double> prices) {
		bins = Map.copyOf(prices);
	}

	/** Item ids by normalized display name, used to price drops that are not in the built-in list. */
	public void setIdsByName(Map<String, String> ids) {
		idsByName = Map.copyOf(ids);
	}

	/**
	 * Adds lowest BINs found by scanning the Auction House, by normalized item name. Names that the
	 * scan did not find this time keep the price they had, so a price never disappears because one
	 * scan missed the item.
	 */
	public synchronized void addNameBins(Map<String, Double> prices) {
		Map<String, Double> merged = new HashMap<>(nameBins);
		merged.putAll(prices);
		nameBins = Map.copyOf(merged);
	}

	public boolean hasBazaar() {
		return !bazaar.isEmpty();
	}

	public boolean hasBins() {
		return !bins.isEmpty();
	}

	public boolean hasNames() {
		return !idsByName.isEmpty();
	}

	/**
	 * What one of this item is worth: the Bazaar price if it is sold there, otherwise the lowest
	 * BIN. Returns NaN when no price is known.
	 */
	public double price(String id, String binId, Mode mode) {
		Bazaar entry = bazaar.get(id);
		if (entry != null) {
			return mode == Mode.SELL_OFFER ? entry.sellOffer() : entry.instantSell();
		}
		Double bin = bins.get(binId != null ? binId : id);
		return bin != null ? bin : Double.NaN;
	}

	/**
	 * What one of this item is worth. If no list has a price right now, the last price that was
	 * ever found is used, so a known item never drops back to a "?".
	 */
	public double price(NucleusItems.Item item, Mode mode) {
		double price = listedPrice(item, mode);
		if (Double.isNaN(price)) {
			price = scanned(item.name());
		}
		return remember(item.name(), mode, price);
	}

	/**
	 * The price from the Bazaar and the lowest BIN list only, without the Auction House scan and
	 * without remembered prices. NaN means the item has to be looked up in the Auction House.
	 */
	public double listedPrice(NucleusItems.Item item, Mode mode) {
		double price = price(item.priceId(), item.binId(), mode);
		if (!Double.isNaN(price)) {
			return price;
		}
		return listedPriceByName(item.name(), mode);
	}

	/** Like {@link #listedPrice(NucleusItems.Item, Mode)} for a drop that is not in the built-in list. */
	public double listedPriceByName(String name, Mode mode) {
		String id = idsByName.get(NucleusItems.normalize(name));
		return id == null ? Double.NaN : price(id, null, mode);
	}

	/** Price of a drop that is not in the built-in list, found by its name. NaN if unknown. */
	public double priceByName(String name, Mode mode) {
		double price = listedPriceByName(name, mode);
		if (Double.isNaN(price)) {
			price = scanned(name);
		}
		return remember(name, mode, price);
	}

	private double scanned(String name) {
		Double auction = nameBins.get(NucleusItems.normalize(name));
		return auction != null ? auction : Double.NaN;
	}

	private double remember(String name, Mode mode, double price) {
		String key = mode.name() + "." + NucleusItems.normalize(name);
		if (!Double.isNaN(price)) {
			lastGood.put(key, price);
			return price;
		}
		Double old = lastGood.get(key);
		return old != null ? old : Double.NaN;
	}

	/** Writes the remembered prices, so they are there again the next time the game starts. */
	public void save(Properties out) {
		lastGood.forEach((key, price) -> out.setProperty("price." + key, Double.toString(price)));
	}

	/** Reads what {@link #save} wrote. */
	public void load(Properties in) {
		for (String name : in.stringPropertyNames()) {
			if (!name.startsWith("price.")) {
				continue;
			}
			try {
				double price = Double.parseDouble(in.getProperty(name).trim());
				if (price > 0d && !Double.isInfinite(price)) {
					lastGood.put(name.substring("price.".length()), price);
				}
			} catch (NumberFormatException ignored) {
				// A damaged line: the price is found again from the lists.
			}
		}
	}

	/**
	 * What it costs to buy one of this item right now: the lowest sell offer at the Bazaar, or
	 * the lowest BIN. Returns NaN when no price is known.
	 */
	public double buyCost(String id) {
		Bazaar entry = bazaar.get(id);
		if (entry != null && entry.sellOffer() > 0) {
			return entry.sellOffer();
		}
		Double bin = bins.get(id);
		return bin != null ? bin : Double.NaN;
	}

	/**
	 * Like {@link #buyCost(String)}, but if the id is not listed the item's name is tried instead.
	 * If there is still no price, the last price that was found is used.
	 */
	public double buyCost(NucleusItems.RunCost cost) {
		double price = buyCost(cost.id());
		if (Double.isNaN(price)) {
			String id = idsByName.get(NucleusItems.normalize(cost.name()));
			price = id == null ? Double.NaN : buyCost(id);
		}
		String key = "BUY." + NucleusItems.normalize(cost.name());
		if (!Double.isNaN(price)) {
			lastGood.put(key, price);
			return price;
		}
		Double old = lastGood.get(key);
		return old != null ? old : Double.NaN;
	}
}
