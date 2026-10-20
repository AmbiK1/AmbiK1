package com.ambik1.booktransfer.text;

/**
 * Состояние форматирования одного куска текста.
 * color — символ цвета Minecraft (0-9, a-f) или 0, если цвет по умолчанию.
 */
public record TextStyle(char color, boolean bold, boolean italic, boolean strike, boolean underline, boolean obfuscated) {
	public static final TextStyle PLAIN = new TextStyle((char) 0, false, false, false, false, false);
	public static final char SECTION = '§';

	public TextStyle withColor(char c) {
		// Как в ванилле: цвет сбрасывает остальные стили.
		return new TextStyle(c, false, false, false, false, false);
	}

	public TextStyle toggle(char flag) {
		return switch (flag) {
			case 'l' -> new TextStyle(color, !bold, italic, strike, underline, obfuscated);
			case 'o' -> new TextStyle(color, bold, !italic, strike, underline, obfuscated);
			case 'm' -> new TextStyle(color, bold, italic, !strike, underline, obfuscated);
			case 'n' -> new TextStyle(color, bold, italic, strike, !underline, obfuscated);
			case 'k' -> new TextStyle(color, bold, italic, strike, underline, !obfuscated);
			default -> this;
		};
	}

	public TextStyle with(char flag) {
		return switch (flag) {
			case 'l' -> new TextStyle(color, true, italic, strike, underline, obfuscated);
			case 'o' -> new TextStyle(color, bold, true, strike, underline, obfuscated);
			case 'm' -> new TextStyle(color, bold, italic, true, underline, obfuscated);
			case 'n' -> new TextStyle(color, bold, italic, strike, true, obfuscated);
			case 'k' -> new TextStyle(color, bold, italic, strike, underline, true);
			default -> this;
		};
	}

	private String flagCodes() {
		StringBuilder sb = new StringBuilder();
		if (obfuscated) sb.append(SECTION).append('k');
		if (bold) sb.append(SECTION).append('l');
		if (strike) sb.append(SECTION).append('m');
		if (underline) sb.append(SECTION).append('n');
		if (italic) sb.append(SECTION).append('o');
		return sb.toString();
	}

	/** Полный набор кодов, чтобы получить этот стиль с нуля (в начале страницы). */
	public String fullCodes() {
		StringBuilder sb = new StringBuilder();
		if (color != 0) sb.append(SECTION).append(color);
		sb.append(flagCodes());
		return sb.toString();
	}

	/** Минимальные коды для перехода из стиля {@code from} в этот. */
	public String transitionFrom(TextStyle from) {
		if (this.equals(from)) return "";
		boolean onlyAdds = color == from.color
				&& (bold || !from.bold) && (italic || !from.italic) && (strike || !from.strike)
				&& (underline || !from.underline) && (obfuscated || !from.obfuscated);
		if (onlyAdds) {
			StringBuilder sb = new StringBuilder();
			if (obfuscated && !from.obfuscated) sb.append(SECTION).append('k');
			if (bold && !from.bold) sb.append(SECTION).append('l');
			if (strike && !from.strike) sb.append(SECTION).append('m');
			if (underline && !from.underline) sb.append(SECTION).append('n');
			if (italic && !from.italic) sb.append(SECTION).append('o');
			return sb.toString();
		}
		// Код цвета сам сбрасывает стили, §r — сбрасывает всё к чёрному тексту книги.
		return (color != 0 ? "" + SECTION + color : SECTION + "r") + flagCodes();
	}
}
