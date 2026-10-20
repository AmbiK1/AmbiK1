package com.ambik1.booktransfer.text;

/** Кусок текста с одним стилем и типом абзаца. */
public record StyledRun(String text, TextStyle style, Kind kind) {
	public enum Kind {
		/** Обычный текст. */
		NORMAL,
		/** Текст, выровненный по центру. */
		CENTER,
		/** Заголовок главы (# Заголовок): по центру, попадает в оглавление. */
		HEADING
	}

	public StyledRun(String text, TextStyle style) {
		this(text, style, Kind.NORMAL);
	}
}
