package zcu.xutil.utils;

import java.io.IOException;
import java.util.BitSet;

import zcu.xutil.XutilRuntimeException;

public class UrlBuilder {
	private static final char[] digits;
	private static final BitSet safeChars;
	static {
		char[] chars = digits = new char[16];
		int i = 0;
		do
			chars[i] = (char) (i < 10 ? i + '0' : i - 10 + 'A');
		while(++i < 16);
		// These characters are specified as unreservered in RFC 2396:
		// "-", "_", ".", "!", "~", "*", "'", "(", ")",
		// "0".."9", "A".."Z", "a".."z"
		// But wait... Java also escapes !, ~, ', (, and )
		// I'm only going to include -, _, ., and * to be consistent with java
		BitSet bitset = safeChars = new BitSet(128);
		bitset.set('A', 'Z' + 1);
		bitset.set('a', 'z' + 1);
		bitset.set('0', '9' + 1);
		bitset.set('-');
		bitset.set('_');
		bitset.set('.');
		bitset.set('*');
	}

	/**
	 * encode octet 0x20, i.e. "space", as "%20" rather than a plus sign
	 *
	 * @see java.net.URLEncoder#encode(String, String)
	 * @param s
	 *            String to encode.
	 * @param encoding
	 *            character encoding to use (e.g., "UTF-8")
	 * @param out
	 *            destination for the encoded string
	 *
	 */
	public static void encode(String s, String encoding, Appendable out) {
		char[] digit = digits;
		BitSet safes = safeChars;
		try {
			for (int c : s.getBytes(encoding == null ? Util.FILE_ENCODING : encoding)) {
				if (c < 0)
					c += 256; // convert from [-128, -1] to [128, 255]
				else if (safes.get(c)) {
					out.append((char) c);
					continue;
				}
				out.append('%').append(digit[c >> 4]).append(digit[c & 0xf]);
			}
		} catch (IOException e) {
			throw new XutilRuntimeException(e);
		}
	}

	public static String encode(String s, String encoding) {
		int len = s.length();
		StringBuilder out = new StringBuilder(len * 2);
		encode(s, encoding, out);
		return len == out.length() ? s : out.toString();
	}

	private final String encode;
	private final String url;
	private StringBuilder buf;

	public UrlBuilder(String baseUrl, String enc) {
		this.encode = enc;
		this.url =baseUrl;
	}

	public UrlBuilder add(String key, String value) {
		encode(value, encode, (buf == null ? (buf = new StringBuilder(url)
					.append(url.indexOf('?') < 0 ? '?' : '&')) : buf.append('&')).append(key).append('='));
		return this;
	}

	@Override
	public String toString() {
		return String.valueOf(buf == null ? url : buf);
	}
}
