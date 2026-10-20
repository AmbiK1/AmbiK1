package com.ambik1.booktransfer.text;

import java.util.HashMap;
import java.util.Map;

/** Посимвольные эффекты: широкий текст, капитель, надстрочные буквы, зеркало, радуга. */
public final class TextEffects {
	private static final Map<Integer, String> SMALL_CAPS = table(
			"abcdefghijklmnopqrstuvwxyz",
			"ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ");
	private static final Map<Integer, String> SUPERSCRIPT = table(
			"abcdefghijklmnoprstuvwxyzABDEGHIJKLMNOPRTUVW0123456789+-=()",
			"ᵃᵇᶜᵈᵉᶠᵍʰⁱʲᵏˡᵐⁿᵒᵖʳˢᵗᵘᵛʷˣʸᶻᴬᴮᴰᴱᴳᴴᴵᴶᴷᴸᴹᴺᴼᴾᴿᵀᵁⱽᵂ⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾");
	private static final Map<Integer, String> MIRROR = table(
			"bdpq()[]{}<>/\\ЯяИиRN?SsZzEЗзJ",
			"dbqp)(][}{><\\/RʁNиЯИ⸮ƧƨƸƹƎƐεᒐ");
	/** Цвета радуги по кругу. */
	public static final String RAINBOW = "c6eab9d";

	private TextEffects() {
	}

	private static Map<Integer, String> table(String from, String to) {
		Map<Integer, String> map = new HashMap<>();
		int[] a = from.codePoints().toArray();
		int[] b = to.codePoints().toArray();
		for (int i = 0; i < Math.min(a.length, b.length); i++) {
			map.put(a[i], new String(Character.toChars(b[i])));
		}
		return map;
	}

	public static String wide(int cp) {
		if (cp > 0x20 && cp < 0x7F) return new String(Character.toChars(cp + 0xFEE0));
		return new String(Character.toChars(cp));
	}

	public static String smallCaps(int cp) {
		return SMALL_CAPS.getOrDefault(Character.toLowerCase(cp), new String(Character.toChars(cp)));
	}

	public static String superscript(int cp) {
		return SUPERSCRIPT.getOrDefault(cp, new String(Character.toChars(cp)));
	}

	/** Зеркальное отражение строки (справа налево). */
	public static String mirror(String s) {
		StringBuilder out = new StringBuilder(s.length());
		int i = s.length();
		while (i > 0) {
			int cp = s.codePointBefore(i);
			i -= Character.charCount(cp);
			out.append(MIRROR.getOrDefault(cp, new String(Character.toChars(cp))));
		}
		return out.toString();
	}
}
