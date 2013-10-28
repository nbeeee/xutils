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
package zcu.xutil.msg.impl;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.utils.ByteArray;

/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class Event implements Externalizable {
	private static final Logger discardLogger = Logger.getLogger(Event.class);
	private static final Object[] EMPTY_PARAMETERS={};
	private static final byte[] EMPTY_BYTES = {};

	private static final byte TYPE_NULL = 0;
	private static final byte TYPE_SERIALIZABLE = 1;
	private static final byte TYPE_BOOLEAN = 10;
	private static final byte TYPE_BYTE = 11;
	private static final byte TYPE_CHAR = 12;
	private static final byte TYPE_DOUBLE = 13;
	private static final byte TYPE_FLOAT = 14;
	private static final byte TYPE_INT = 15;
	private static final byte TYPE_LONG = 16;
	private static final byte TYPE_SHORT = 17;
	private static final byte TYPE_STRING = 18;
	private static final byte TYPE_BYTEARRAY = 19;

	public static byte[] marshall(Object rsp) {
		if (rsp == null)
			return EMPTY_BYTES;
		ByteArray os = new ByteArray(512);
		int b = index(rsp.getClass());
		os.write(b);
		ObjectOutputStream oos = null;
		try {
			if (b == TYPE_SERIALIZABLE)
				(oos = new ObjectOutputStream(os)).writeObject(rsp);
			else
				primitiveWrite(b, rsp, new DataOutputStream(os));
		} catch (IOException e) {
			throw new MSGException(e.toString());
		} finally {
			Objutil.closeQuietly(oos);
		}
		return os.toByteArray();
	}

	public static Object unmarshall(InputStream is) throws Throwable {
		Object o;
		ObjectInputStream ois = null;
		try {
			int b = is.read();
			if (b <= 0)
				return null;
			if (b == TYPE_SERIALIZABLE)
				o = (ois = new ObjectInputStream(is)).readObject();
			else
				o = primitiveRead(b, new DataInputStream(is));
		} catch (IOException e) {
			throw new MSGException(e.toString());
		} catch (ClassNotFoundException e) {
			throw new MSGException(e.toString());
		} finally {
			Objutil.closeQuietly(ois);
		}
		if (o instanceof Throwable)
			throw (Throwable) o;
		return o;
	}

	private static int index(Class type) {
		if (type == String.class)
			return TYPE_STRING;
		if (type == Boolean.class)
			return TYPE_BOOLEAN;
		if (type == Byte.class)
			return TYPE_BYTE;
		if (type == Character.class)
			return TYPE_CHAR;
		if (type == Double.class)
			return TYPE_DOUBLE;
		if (type == Float.class)
			return TYPE_FLOAT;
		if (type == Integer.class)
			return TYPE_INT;
		if (type == Long.class)
			return TYPE_LONG;
		if (type == Short.class)
			return TYPE_SHORT;
		if (type == byte[].class)
			return TYPE_BYTEARRAY;
		return TYPE_SERIALIZABLE;
	}

	private static Object primitiveRead(int b, DataInputStream dis) throws IOException {
		switch (b) {
		case TYPE_BOOLEAN:
			return dis.readBoolean();
		case TYPE_BYTE:
			return dis.readByte();
		case TYPE_CHAR:
			return dis.readChar();
		case TYPE_DOUBLE:
			return dis.readDouble();
		case TYPE_FLOAT:
			return dis.readFloat();
		case TYPE_INT:
			return dis.readInt();
		case TYPE_LONG:
			return dis.readLong();
		case TYPE_SHORT:
			return dis.readShort();
		case TYPE_STRING:
			return dis.readUTF();
		case TYPE_BYTEARRAY:
			byte[] buf = new byte[dis.readInt()];
			dis.readFully(buf);
			return buf;
		default:
			throw new IllegalMsgException("type " + b + " is illegal");
		}
	}

	private static void primitiveWrite(int b, Object obj, DataOutputStream dos) throws IOException {
		switch (b) {
		case TYPE_BOOLEAN:
			dos.writeBoolean((Boolean) obj);
			return;
		case TYPE_BYTE:
			dos.writeByte((Byte) obj);
			return;
		case TYPE_CHAR:
			dos.writeChar((Character) obj);
			return;
		case TYPE_DOUBLE:
			dos.writeDouble((Double) obj);
			return;
		case TYPE_FLOAT:
			dos.writeFloat((Float) obj);
			return;
		case TYPE_INT:
			dos.writeInt((Integer) obj);
			return;
		case TYPE_LONG:
			dos.writeLong((Long) obj);
			return;
		case TYPE_SHORT:
			dos.writeShort((Short) obj);
			return;
		case TYPE_STRING:
			dos.writeUTF((String) obj);
			return;
		case TYPE_BYTEARRAY:
			byte[] buf = (byte[]) obj;
			dos.writeInt(buf.length);
			dos.write(buf);
			return;
		default:
			throw new IllegalMsgException("type " + b + " is illegal");
		}
	}

	boolean syncall;
	private String name;
	private String value;
	private byte[] datas;
	private Date expire;

	private transient Long id;
	private transient volatile Object[] objects;
	transient Event next;

	public Event() {
		// default
	}

	public Event(String eventName, String eventValue, Object... parameters) {
		this.name = eventName;
		this.value = eventValue;
		this.objects = parameters == null ? EMPTY_PARAMETERS : parameters;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long l) {
		this.id = l;
	}

	public String getName() {
		return name;
	}

	public void setName(String s) {
		name = s;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String s) {
		value = s;
	}

	public byte[] getDatas() {
		if (datas == null) {
			if (objects == null || objects.length == 0)
				datas = EMPTY_BYTES;
			else {
				ByteArray os = new ByteArray(512);
				ObjectOutputStream oos = null;
				try {
					DataOutputStream dos = null;
					for (Object obj : objects) {
						if (obj == null)
							os.write(TYPE_NULL);
						else {
							int b = index(obj.getClass());
							os.write(b);
							if (b == TYPE_SERIALIZABLE) {
								if (oos == null)
									oos = new ObjectOutputStream(os);
								oos.writeObject(obj);
								oos.flush();
							} else {
								if (dos == null)
									dos = new DataOutputStream(os);
								primitiveWrite(b, obj, dos);
							}
						}
					}
				} catch (IOException e) {
					throw new IllegalMsgException(e.toString());
				} finally {
					Objutil.closeQuietly(oos);
				}
				datas = os.toByteArray();
			}
		}
		return datas;
	}

	public void setDatas(byte[] bytes) {
		datas = bytes;
		objects = null;
	}

	public Date getExpire() {
		return expire;
	}

	public void setExpire(Date date) {
		this.expire = date;
	}

	private static void deserial(byte[] bytes, List<Object> list) {
		ObjectInputStream ois = null;
		try {
			DataInputStream dis = null;
			InputStream is = ByteArray.toStream(bytes);
			int b;
			while ((b = is.read()) >= 0) {
				if (b == TYPE_NULL)
					list.add(null);
				else if (b == TYPE_SERIALIZABLE) {
					if (ois == null)
						ois = new ObjectInputStream(is);
					list.add(ois.readObject());
				} else {
					if (dis == null)
						dis = new DataInputStream(is);
					list.add(primitiveRead(b, dis));
				}
			}
		} catch (IOException e) {
			throw new IllegalMsgException(e.toString());
		} catch (ClassNotFoundException e) {
			throw new IllegalMsgException(e.toString());
		} finally {
			Objutil.closeQuietly(ois);
		}
	}

	Object[] parameters() {
		if (objects == null) {
			if (datas == null || datas.length == 0)
				objects = EMPTY_PARAMETERS;
			else {
				List<Object> list = new ArrayList<Object>();
				deserial(datas, list);
				objects = list.toArray();
			}
		}
		return objects;
	}
	@Override
	public final void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		readFrom(in);
	}
	@Override
	public final void writeExternal(ObjectOutput out) throws IOException {
		writeTo(out);
	}

	public void readFrom(DataInput in) throws IOException {
		int ctrl = in.readInt();
		syncall = (ctrl & 0x80000000) != 0;
		if ((ctrl & 0x40000000) != 0)
			name = in.readUTF();
		if ((ctrl & 0x20000000) != 0)
			value = in.readUTF();
		if ((ctrl & 0x10000000) != 0)
			expire = new Date(in.readLong());
		byte[] buf = new byte[ctrl & 0x0fffffff];
		in.readFully(buf);
		setDatas(buf);
	}

	public void writeTo(DataOutput out) throws IOException {
		byte[] buf = getDatas();
		int ctrl = buf.length;
		if (ctrl > 0x0fffffff)
			throw new IllegalMsgException("too larger datas:" + ctrl);
		if (syncall)
			ctrl |= 0x80000000;
		if (name != null)
			ctrl |= 0x40000000;
		if (value != null)
			ctrl |= 0x20000000;
		if (expire != null)
			ctrl |= 0x10000000;
		out.writeInt(ctrl);
		if (name != null)
			out.writeUTF(name);
		if (value != null)
			out.writeUTF(value);
		if (expire != null)
			out.writeLong(expire.getTime());
		out.write(buf);
	}

	@Override
	public String toString() {
		return name + " ,value=" + value;
	}

	void discardLogger(String cause) {
		List<Object> list;
		if (objects != null)
			list = Arrays.asList(objects);
		else if (datas == null || datas.length == 0)
			list = Collections.emptyList();
		else {
			list = new ArrayList<Object>();
			try {
				deserial(datas, list);
			} catch (Throwable ex) {
				// ignore
			}
		}
		discardLogger.info("{}: name={} ,value={} ,params={}", name, value, list);
	}
}
