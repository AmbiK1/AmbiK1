package com.ambik1.booktransfer.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Разбирает разметку пользователя:
 * <ul>
 *   <li>коды Minecraft через &amp;: &amp;l жирный, &amp;o курсив, &amp;m зачёркнутый, &amp;n подчёркнутый,
 *       &amp;k меняющийся, &amp;r сброс, &amp;0-&amp;f цвета; &amp;&amp; — сам символ &amp;;</li>
 *   <li>markdown: **жирный**, *курсив*, ~~зачёркнутый~~, __подчёркнутый__, ||меняющийся||;</li>
 *   <li>теги: [center], [rainbow], [wide], [small], [sup], [mirror], [flip] (и русские названия),
 *       [/] — сброс стиля, [имя] — пользовательский шаблон;</li>
 *   <li># в начале строки — заголовок главы;</li>
 *   <li>\ перед символом отменяет его особый смысл.</li>
 * </ul>
 */
public final class MarkupParser {
	private enum Tag { CENTER, RAINBOW, WIDE, SMALL, SUP, MIRROR, FLIP }

	private static final Map<String, Tag> TAGS = Map.ofEntries(
			Map.entry("center", Tag.CENTER), Map.entry("центр", Tag.CENTER),
			Map.entry("rainbow", Tag.RAINBOW), Map.entry("радуга", Tag.RAINBOW),
			Map.entry("wide", Tag.WIDE), Map.entry("широкий", Tag.WIDE),
			Map.entry("small", Tag.SMALL), Map.entry("капитель", Tag.SMALL),
			Map.entry("sup", Tag.SUP), Map.entry("мелкий", Tag.SUP),
			Map.entry("mirror", Tag.MIRROR), Map.entry("зеркало", Tag.MIRROR),
			Map.entry("flip", Tag.FLIP), Map.entry("перевернуть", Tag.FLIP));

	private final Map<String, String> presets;
	private final List<StyledRun> runs = new ArrayList<>();
	private final StringBuilder buf = new StringBuilder();
	private TextStyle style;
	private boolean center;
	private boolean heading;
	private TextStyle beforeHeading;
	private boolean rainbow;
	private boolean wide;
	private boolean small;
	private boolean sup;
	private int rainbowIndex;

	private MarkupParser(Map<String, String> presets, TextStyle style) {
		this.presets = presets;
		this.style = style;
	}

	public static List<StyledRun> parse(String input, boolean formatting) {
		return parse(input, formatting, Map.of());
	}

	/** @param presets пользовательские шаблоны: имя (в нижнем регистре) → коды вида "&amp;6&amp;l" */
	public static List<StyledRun> parse(String input, boolean formatting, Map<String, String> presets) {
		String text = normalize(input);
		if (!formatting) {
			List<StyledRun> plain = new ArrayList<>();
			if (!text.isEmpty()) plain.add(new StyledRun(text, TextStyle.PLAIN));
			return plain;
		}
		MarkupParser parser = new MarkupParser(presets, TextStyle.PLAIN);
		parser.run(text);
		return parser.runs;
	}

	private void run(String text) {
		int i = 0;
		int n = text.length();
		while (i < n) {
			char c = text.charAt(i);
			char next = i + 1 < n ? text.charAt(i + 1) : 0;
			boolean lineStart = i == 0 || text.charAt(i - 1) == '\n';

			if (c == '\\' && i + 1 < n && "\\*~_|&[#".indexOf(next) >= 0) {
				append(next);
				i += 2;
				continue;
			}
			if (lineStart && c == '#' && next == ' ') {
				flush();
				heading = true;
				beforeHeading = style;
				style = style.with('l');
				i += 2;
				continue;
			}
			if (c == '\n') {
				flush();
				if (heading) {
					heading = false;
					style = beforeHeading;
				}
				buf.append('\n');
				flush();
				i++;
				continue;
			}
			if (c == '&' && next == '&') {
				append('&');
				i += 2;
				continue;
			}
			if ((c == '&' || c == TextStyle.SECTION) && isCode(next)) {
				setStyle(style.apply(next));
				i += 2;
				continue;
			}
			if (c == '[') {
				int consumed = tryTag(text, i);
				if (consumed > 0) {
					i += consumed;
					continue;
				}
			}
			TextStyle toggled = null;
			int len = 2;
			if (c == '*' && next == '*') toggled = style.toggle('l');
			else if (c == '*') {
				toggled = style.toggle('o');
				len = 1;
			} else if (c == '~' && next == '~') toggled = style.toggle('m');
			else if (c == '_' && next == '_') toggled = style.toggle('n');
			else if (c == '|' && next == '|') toggled = style.toggle('k');

			if (toggled != null) {
				setStyle(toggled);
				i += len;
				continue;
			}

			int cp = text.codePointAt(i);
			appendCodePoint(cp);
			i += Character.charCount(cp);
		}
		flush();
	}

	private static boolean isCode(char c) {
		c = Character.toLowerCase(c);
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || "klmnor".indexOf(c) >= 0 && c != 0;
	}

	/** Пытается разобрать тег в позиции i. Возвращает число съеденных символов или 0. */
	private int tryTag(String text, int i) {
		int end = text.indexOf(']', i);
		if (end < 0 || end - i > 32) return 0;
		String raw = text.substring(i + 1, end);
		if (raw.contains("\n")) return 0;
		boolean closing = raw.startsWith("/");
		String name = (closing ? raw.substring(1) : raw).trim().toLowerCase(Locale.ROOT);
		int consumed = end - i + 1;

		if (closing && name.isEmpty()) {
			setStyle(TextStyle.PLAIN);
			return consumed;
		}
		Tag tag = TAGS.get(name);
		if (tag != null) {
			if (!closing && (tag == Tag.MIRROR || tag == Tag.FLIP)) {
				return reversedBlock(text, i, end, name, tag);
			}
			flush();
			switch (tag) {
				case CENTER -> center = !closing;
				case RAINBOW -> {
					rainbow = !closing;
					rainbowIndex = 0;
				}
				case WIDE -> wide = !closing;
				case SMALL -> small = !closing;
				case SUP -> sup = !closing;
				default -> {
					// Закрывающий [/flip] без открывающего — просто убираем.
				}
			}
			return consumed;
		}
		String codes = presets.get(name);
		if (codes != null) {
			if (closing) {
				setStyle(TextStyle.PLAIN);
			} else {
				TextStyle s = style;
				for (int k = 0; k + 1 < codes.length(); k++) {
					char ch = codes.charAt(k);
					if ((ch == '&' || ch == TextStyle.SECTION) && isCode(codes.charAt(k + 1))) {
						s = s.apply(codes.charAt(k + 1));
						k++;
					}
				}
				setStyle(s);
			}
			return consumed;
		}
		return 0;
	}

	/** [flip]…[/flip] и [mirror]…[/mirror]: разбираем внутренность отдельно и разворачиваем. */
	private int reversedBlock(String text, int start, int tagEnd, String name, Tag tag) {
		String lower = text.toLowerCase(Locale.ROOT);
		int close = lower.indexOf("[/" + name + "]", tagEnd);
		int innerEnd = close < 0 ? text.length() : close;
		String inner = text.substring(tagEnd + 1, innerEnd);

		flush();
		MarkupParser sub = new MarkupParser(presets, style);
		sub.center = center;
		sub.rainbow = rainbow;
		sub.wide = wide;
		sub.small = small;
		sub.sup = sup;
		sub.run(inner);
		List<StyledRun> transformed = new ArrayList<>();
		for (StyledRun r : sub.runs) {
			String t = tag == Tag.FLIP ? UpsideDown.flip(r.text()) : TextEffects.mirror(r.text());
			transformed.add(new StyledRun(t, r.style(), r.kind()));
		}
		java.util.Collections.reverse(transformed);
		runs.addAll(transformed);
		style = sub.style;

		int afterClose = close < 0 ? text.length() : close + name.length() + 3;
		return afterClose - start;
	}

	private void setStyle(TextStyle s) {
		if (!s.equals(style)) {
			flush();
			style = s;
		}
	}

	private void append(char c) {
		appendCodePoint(c);
	}

	private void appendCodePoint(int cp) {
		String s;
		if (wide) s = TextEffects.wide(cp);
		else if (small) s = TextEffects.smallCaps(cp);
		else if (sup) s = TextEffects.superscript(cp);
		else s = new String(Character.toChars(cp));

		if (rainbow && !Character.isWhitespace(cp)) {
			flush();
			TextStyle saved = style;
			style = style.recolor(TextEffects.RAINBOW.charAt(rainbowIndex++ % TextEffects.RAINBOW.length()));
			buf.append(s);
			flush();
			style = saved;
			return;
		}
		buf.append(s);
	}

	private void flush() {
		if (buf.length() == 0) return;
		StyledRun.Kind kind = heading ? StyledRun.Kind.HEADING : center ? StyledRun.Kind.CENTER : StyledRun.Kind.NORMAL;
		String text = buf.toString();
		buf.setLength(0);
		if (text.equals("\n")) kind = StyledRun.Kind.NORMAL;
		if (!runs.isEmpty()) {
			StyledRun last = runs.get(runs.size() - 1);
			if (last.style().equals(style) && last.kind() == kind && !text.equals("\n") && !last.text().endsWith("\n")) {
				runs.set(runs.size() - 1, new StyledRun(last.text() + text, style, kind));
				return;
			}
		}
		runs.add(new StyledRun(text, style, kind));
	}

	/** Приводит переводы строк и табы к виду, понятному книге. */
	static String normalize(String s) {
		return s.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ")
				.replace(' ', ' ').replace("﻿", "");
	}

	/** Превращает §-коды обратно в &amp;-разметку (для копирования остатка текста). */
	public static String toAmpersand(String s) {
		return s.replace("&", "&&").replace(TextStyle.SECTION, '&');
	}
}
