package zcu.xutil.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;

import zcu.xutil.XutilRuntimeException;

public class Base64 {
	private static final byte[] charOf, byteOf;
	static {
		byte[] CtoB = byteOf= new byte[128], BtoC = charOf =new byte[64];
		int i = 0;
		do
			BtoC[i] = (byte) (i < 26 ? i + 'A' : (i < 52 ? i - 26 + 'a' : i - 52 + '0'));
		while(++i < 62);
		BtoC[62] = '+';
		BtoC[63] = '/';
		Arrays.fill(CtoB, (byte) -1);
		i = 0;
		do
			CtoB[BtoC[i]] = (byte) i;
		while(++i < 64);
	}
	private final OutputStream out;
	private final boolean linefeed;
	private final ByteArray buf;
	private int charCount = 0, carryOver = 0;

	private Base64(OutputStream output, boolean lineFeed) {
		this.out = output;
		this.linefeed = lineFeed;
		this.buf = output instanceof ByteArray ? (ByteArray) output : new ByteArray();
	}

	private void encode(byte[] bytes, int offset, int len) throws IOException {
		// if (offset < 0 || len < 0 || (offset + len) > bytes.length)
		// throw new IndexOutOfBoundsException();
		for (int mod, i = 0; i < len; i++) {
			int x = bytes[offset + i] & 0xff;
			if ((mod = charCount % 3) == 0) {
				buf.write(charOf[x >> 2]);
				carryOver = x & 3; // last two bits
			} else if (mod == 1) {
				buf.write(charOf[((carryOver << 4) + (x >> 4)) & 63]);
				carryOver = x & 15; // last four bits
			} else {
				buf.write(charOf[((carryOver << 2) + (x >> 6)) & 63]);
				buf.write(charOf[x & 63]); // last six bits
			}
			charCount++;
			// Add newline every 76 output chars (that's 57 input chars)
			if (linefeed && charCount % 57 == 0)
				buf.write('\n');
			if (buf != out) {
				buf.writeTo(out);
				buf.reset();
			}
		}
	}

	private void decode(char[] chars, int offset, int len) throws IOException {
		// if (offset < 0 || len < 0 || (offset + len) > chars.length)
		// throw new IndexOutOfBoundsException();
		for (int x, mod, i = 0; i < len && (x = chars[offset + i]) != '='; i++) {
			if (x > 127 || (x = byteOf[x]) < 0)
				continue;
			if ((mod = charCount % 4) == 0)
				carryOver = x & 63;
			else if (mod == 1) {
				buf.write(((carryOver << 2) + (x >> 4)) & 255);
				carryOver = x & 15;
			} else if (mod == 2) {
				buf.write(((carryOver << 4) + (x >> 2)) & 255);
				carryOver = x & 3;
			} else
				buf.write(((carryOver << 6) + x) & 255);
			charCount++;
		}
		if (buf != out) {
			buf.writeTo(out);
			buf.reset();
		}
	}

	private void doFinal() throws IOException {
		int mod = charCount % 3;
		if (mod == 0)
			return;
		if (mod == 2) // two leftovers
			buf.write(charOf[(carryOver << 2) & 63]);
		else {// one leftover
			buf.write(charOf[(carryOver << 4) & 63]);
			buf.write('=');
		}
		buf.write('=');
		if (buf != out) {
			buf.writeTo(out);
			buf.reset();
		}
	}

	public static byte[] decode(String str) {
		try {
			char[] chars = str.toCharArray();
			ByteArray out = new ByteArray(chars.length);
			Base64 base64 = new Base64(out, false);
			base64.decode(chars, 0, chars.length);
			return out.toByteArray();
		} catch (IOException e) {
			throw new XutilRuntimeException(e);
		}
	}

	public static byte[] decode(Reader in) throws IOException {
		ByteArray out = new ByteArray(256);
		decode(in, out);
		return out.toByteArray();
	}

	public static void decode(Reader in, OutputStream out) throws IOException {
		Base64 base64 = new Base64(out, false);
		char[] buf = new char[1024];
		int size;
		while ((size = in.read(buf)) > 0)
			base64.decode(buf, 0, size);
	}

	public static String encode(byte[] bytes, boolean linefeed) {
		ByteArray out = new ByteArray((int) (bytes.length * 1.37));
		Base64 base64 = new Base64(out, linefeed);
		try {
			base64.encode(bytes, 0, bytes.length);
			base64.doFinal();
			return out.toString("ascii");
		} catch (IOException e) {
			throw new XutilRuntimeException(e);
		}
	}

	public static String encode(InputStream in, boolean linefeed) throws IOException {
		ByteArray out = new ByteArray(256);
		encode(in, linefeed, out);
		return out.toString("ascii");
	}

	public static void encode(InputStream in, boolean linefeed, OutputStream out) throws IOException {
		Base64 base64 = new Base64(out, linefeed);
		byte[] buf = new byte[1024];
		int size;
		while ((size = in.read(buf)) > 0)
			base64.encode(buf, 0, size);
		base64.doFinal();
	}
}
