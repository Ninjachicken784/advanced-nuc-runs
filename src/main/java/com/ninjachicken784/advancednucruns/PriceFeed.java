package com.ninjachicken784.advancednucruns;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Reads the price lists the game's public web services send: the Bazaar, the lowest BIN list, and
 * the list of item names. Contains no Minecraft code, so it can be tested on its own with plain
 * text. A list that cannot be read throws, so the caller keeps the older prices.
 */
public final class PriceFeed {
	private PriceFeed() { }

	/**
	 * Both Bazaar prices of every item. The sell offer is the lowest price someone is selling at and
	 * the instant sell is the highest price someone is buying at, both from the top of the order
	 * book. When one side of the book is empty, the quick-status price is used instead.
	 */
	public static Map<String, PriceBook.Bazaar> parseBazaar(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		if (!root.has("products") || !root.get("products").isJsonObject()) {
			throw new IllegalArgumentException("The Bazaar list has no products");
		}

		Map<String, PriceBook.Bazaar> prices = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("products").entrySet()) {
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			JsonObject product = entry.getValue().getAsJsonObject();
			JsonObject quick = product.has("quick_status") && product.get("quick_status").isJsonObject()
				? product.getAsJsonObject("quick_status") : null;

			double sellOffer = topOfBook(product, "sell_summary");
			double buyOrder = topOfBook(product, "buy_summary");
			if (!(sellOffer > 0d) && quick != null) {
				sellOffer = number(quick, "buyPrice");
			}
			if (!(buyOrder > 0d) && quick != null) {
				buyOrder = number(quick, "sellPrice");
			}
			if (!(sellOffer > 0d) && !(buyOrder > 0d)) {
				continue;
			}
			if (!(sellOffer > 0d)) {
				sellOffer = buyOrder;
			}
			if (!(buyOrder > 0d)) {
				buyOrder = sellOffer;
			}
			prices.put(entry.getKey(), PriceBook.bazaarOf(sellOffer, buyOrder));
		}
		return prices;
	}

	/** The lowest BIN of every item on the Auction House, by item id. */
	public static Map<String, Double> parseBins(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		Map<String, Double> prices = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			JsonElement value = entry.getValue();
			if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() && value.getAsDouble() > 0d) {
				prices.put(entry.getKey(), value.getAsDouble());
			}
		}
		return prices;
	}

	/** One page of the Auction House: how many pages there are, and the lowest BIN of the wanted items on this page. */
	public record AuctionPage(int totalPages, Map<String, Double> lowestByName) { }

	/**
	 * Reads one page of the Hypixel auction list and keeps the lowest buy-it-now price of every
	 * wanted item, by normalized item name. Only auctions that are BINs count.
	 *
	 * @param wanted normalized item names to look for
	 */
	public static AuctionPage parseAuctionPage(String json, Set<String> wanted) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		if (!root.has("auctions") || !root.get("auctions").isJsonArray()) {
			throw new IllegalArgumentException("The auction page has no auctions");
		}
		int totalPages = root.has("totalPages") && root.get("totalPages").isJsonPrimitive()
			? root.get("totalPages").getAsInt() : 1;

		Map<String, Double> lowest = new HashMap<>();
		for (JsonElement element : root.getAsJsonArray("auctions")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject auction = element.getAsJsonObject();
			boolean bin = auction.has("bin") && auction.get("bin").isJsonPrimitive() && auction.get("bin").getAsBoolean();
			if (!bin || !auction.has("item_name") || !auction.get("item_name").isJsonPrimitive()) {
				continue;
			}
			String name = NucleusItems.normalize(auction.get("item_name").getAsString());
			double price = number(auction, "starting_bid");
			if (price > 0d && wanted.contains(name)) {
				lowest.merge(name, price, Math::min);
			}
		}
		return new AuctionPage(totalPages, lowest);
	}

	/** Item ids by normalized display name, so a drop can be priced by its name. */
	public static Map<String, String> parseNames(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		if (!root.has("items") || !root.get("items").isJsonArray()) {
			throw new IllegalArgumentException("The item list has no items");
		}

		Map<String, String> ids = new HashMap<>();
		for (JsonElement element : root.getAsJsonArray("items")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject item = element.getAsJsonObject();
			if (item.has("id") && item.has("name") && item.get("id").isJsonPrimitive() && item.get("name").isJsonPrimitive()) {
				String name = NucleusItems.normalize(item.get("name").getAsString());
				if (!name.isEmpty()) {
					ids.putIfAbsent(name, item.get("id").getAsString());
				}
			}
		}
		return ids;
	}

	/** The price of the first order in a list of orders, or 0 if there are none. */
	private static double topOfBook(JsonObject product, String listName) {
		if (!product.has(listName) || !product.get(listName).isJsonArray()) {
			return 0d;
		}
		JsonArray orders = product.getAsJsonArray(listName);
		if (orders.isEmpty() || !orders.get(0).isJsonObject()) {
			return 0d;
		}
		return number(orders.get(0).getAsJsonObject(), "pricePerUnit");
	}

	private static double number(JsonObject object, String name) {
		if (!object.has(name) || !object.get(name).isJsonPrimitive() || !object.get(name).getAsJsonPrimitive().isNumber()) {
			return 0d;
		}
		return object.get(name).getAsDouble();
	}
}
