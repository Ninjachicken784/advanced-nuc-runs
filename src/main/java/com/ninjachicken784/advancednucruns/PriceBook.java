package com.ninjachicken784.advancednucruns;

import java.util.Map;

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

	public double price(NucleusItems.Item item, Mode mode) {
		double price = price(item.priceId(), item.binId(), mode);
		// If the id is not one the game lists, try the item's name instead.
		return Double.isNaN(price) ? priceByName(item.name(), mode) : price;
	}

	/** Price of a drop that is not in the built-in list, found by its name. NaN if unknown. */
	public double priceByName(String name, Mode mode) {
		String id = idsByName.get(NucleusItems.normalize(name));
		return id == null ? Double.NaN : price(id, null, mode);
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

	/** Like {@link #buyCost(String)}, but if the id is not listed the item's name is tried instead. */
	public double buyCost(NucleusItems.RunCost cost) {
		double price = buyCost(cost.id());
		if (!Double.isNaN(price)) {
			return price;
		}
		String id = idsByName.get(NucleusItems.normalize(cost.name()));
		return id == null ? Double.NaN : buyCost(id);
	}
}
