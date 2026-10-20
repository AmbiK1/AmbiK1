package com.ambik1.booktransfer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Настройки мода, хранятся в config/booktransfer.json. */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("booktransfer.json");
	private static ModConfig instance;

	public boolean appendMode = false;
	public boolean formatting = true;
	public boolean upsideDown = false;
	public boolean pageNumbers = false;
	public boolean tableOfContents = false;
	public boolean chapterNewPage = true;
	public boolean multiVolume = true;
	public boolean rememberDraft = true;
	/** Шаблоны стилей: имя → коды вида "&amp;6&amp;l". */
	public Map<String, String> presets = defaultPresets();

	private static Map<String, String> defaultPresets() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("заголовок", "&6&l");
		map.put("важно", "&c&l");
		map.put("цитата", "&8&o");
		return map;
	}

	public static ModConfig get() {
		if (instance == null) load();
		return instance;
	}

	public static void load() {
		ModConfig cfg = null;
		if (Files.isRegularFile(FILE)) {
			try {
				cfg = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), ModConfig.class);
			} catch (Exception e) {
				BookTransferClient.LOGGER.warn("Не удалось прочитать {}: {}", FILE, e.toString());
			}
		}
		if (cfg == null) cfg = new ModConfig();
		if (cfg.presets == null) cfg.presets = defaultPresets();
		instance = cfg;
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (IOException e) {
			BookTransferClient.LOGGER.warn("Не удалось сохранить {}: {}", FILE, e.toString());
		}
	}

	/** Шаблоны с ключами в нижнем регистре — для разбора текста. */
	public Map<String, String> presetsForParser() {
		Map<String, String> map = new LinkedHashMap<>();
		presets.forEach((k, v) -> map.put(k.toLowerCase(Locale.ROOT), v));
		return map;
	}
}
