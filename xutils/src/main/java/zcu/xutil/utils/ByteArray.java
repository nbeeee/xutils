/*
 * Copyright 2009 zaichu xiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zcu.xutil.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public final class ByteArray extends OutputStream {
	public static InputStream toStream(byte buf[]) {
		return toStream(buf, 0, buf.length);
	}

	public static InputStream toStream(byte buf[], int offset, int length) {
		return new ByteArrayInputStream(buf, offset, length) {
			@Override
			public int read() {
				return (pos < count) ? (buf[pos++] & 0xff) : -1;
			}

			@Override
			public int read(byte b[], int off, int len) {
				if (off < 0 || len < 0 || len > b.length - off)
					throw new IndexOutOfBoundsException();
				if (pos >= count)
					return -1;
				if (pos + len > count)
					len = count - pos;
				if (len <= 0)
					return 0;
				System.arraycopy(buf, pos, b, off, len);
				pos += len;
				return len;
			}

			@Override
			public long skip(long n) {
				if (pos + n > count)
					n = count - pos;
				if (n <= 0)
					return 0;
				pos += n;
				return n;
			}

			@Override
			public int available() {
				return count - pos;
			}

			@Override
			public void reset() {
				pos = mark;
			}
		};
	}

	private byte buf[];
	private int count;

	public ByteArray() {
		this.buf = new byte[32];
	}

	public ByteArray(int size) {
		if (size < 0)
			throw new IllegalArgumentException("size < 0");
		this.buf = new byte[size];
	}

	@Override
	public void write(int b) {
		int newcount = count + 1;
		if (newcount > buf.length) {
			byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
			System.arraycopy(buf, 0, newbuf, 0, count);
			buf = newbuf;
		}
		buf[count] = (byte) b;
		count = newcount;
	}

	@Override
	public void write(byte b[], int off, int len) {
		if (off < 0 || len < 0 || off + len > b.length || off + len < 0)
			throw new IndexOutOfBoundsException();
		else if (len == 0)
			return;
		int newcount = count + len;
		if (newcount > buf.length) {
			byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
			System.arraycopy(buf, 0, newbuf, 0, count);
			buf = newbuf;
		}
		System.arraycopy(b, off, buf, count, len);
		count = newcount;
	}

	public void writeTo(OutputStream out) throws IOException {
		out.write(buf, 0, count);
	}

	public void reset() {
		count = 0;
	}

	public byte[] toByteArray() {
		byte newbuf[] = new byte[count];
		System.arraycopy(buf, 0, newbuf, 0, count);
		return newbuf;
	}

	public int size() {
		return count;
	}

	public byte[] getRawBuffer(){
		return buf;
	}

	public InputStream asInput() {
		return toStream(buf, 0, count);
	}

	@Override
	public String toString() {
		return new String(buf, 0, count);
	}

	public String toString(String enc) throws UnsupportedEncodingException {
		return new String(buf, 0, count, enc);
	}
}
