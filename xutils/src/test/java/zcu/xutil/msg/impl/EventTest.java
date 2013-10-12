package zcu.xutil.msg.impl;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;
import zcu.xutil.msg.DMCache;
import zcu.xutil.msg.DMCache.Alias;
import zcu.xutil.msg.impl.Event;
import zcu.xutil.sql.DBType;
import zcu.xutil.txm.test.User;
import zcu.xutil.utils.ByteArray;
import zcu.xutil.utils.Function;

public class EventTest implements Serializable {
	private static final long serialVersionUID = -6558967736631681889L;
	private static Map<Class, Byte> PRIMITIVE_TYPES = new HashMap<Class, Byte>();

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

	static {
		PRIMITIVE_TYPES.put(Boolean.class, TYPE_BOOLEAN);
		PRIMITIVE_TYPES.put(Byte.class, TYPE_BYTE);
		PRIMITIVE_TYPES.put(Character.class, TYPE_CHAR);
		PRIMITIVE_TYPES.put(Double.class, TYPE_DOUBLE);
		PRIMITIVE_TYPES.put(Float.class, TYPE_FLOAT);
		PRIMITIVE_TYPES.put(Integer.class, TYPE_INT);
		PRIMITIVE_TYPES.put(Long.class, TYPE_LONG);
		PRIMITIVE_TYPES.put(Short.class, TYPE_SHORT);
		PRIMITIVE_TYPES.put(String.class, TYPE_STRING);
		PRIMITIVE_TYPES.put(byte[].class, TYPE_BYTEARRAY);
	}
//	private static Object[] toObjects(byte[] buffer, int offset, int length) {
//	if (length == 0)
//		return EMPTY_OBJECTS;
//	DataInputStream in = new DataInputStream(ByteArray.toStream(buffer, offset, length));
//	ObjectInputStream ois = null;
//	try {
//		List<Object> list = new ArrayList<Object>();
//		while ((length = in.read()) >= 0) {
//			switch (length) {
//			case TYPE_NULL:
//				list.add(null);
//				break;
//			case TYPE_BOOLEAN:
//				list.add(in.readBoolean());
//				break;
//			case TYPE_BYTE:
//				list.add(in.readByte());
//				break;
//			case TYPE_CHAR:
//				list.add(in.readChar());
//				break;
//			case TYPE_DOUBLE:
//				list.add(in.readDouble());
//				break;
//			case TYPE_FLOAT:
//				list.add(in.readFloat());
//				break;
//			case TYPE_INT:
//				list.add(in.readInt());
//				break;
//			case TYPE_LONG:
//				list.add(in.readLong());
//				break;
//			case TYPE_SHORT:
//				list.add(in.readShort());
//				break;
//			case TYPE_STRING:
//				list.add(in.readUTF());
//				break;
//			case TYPE_BYTEARRAY:
//				byte[] buf = new byte[in.readInt()];
//				in.readFully(buf);
//				list.add(buf);
//				break;
//			case TYPE_SERIALIZABLE:
//				if (ois == null)
//					ois = new ObjectInputStream(in);
//				list.add(ois.readObject());
//				break;
//			default:
//				throw new IllegalArgumentException("type " + length + " is illegal");
//			}
//		}
//		return list.toArray();
//	} catch (IOException e) {
//		throw new XutilRuntimeException(e);
//	} catch (ClassNotFoundException e) {
//		throw new XutilRuntimeException(e);
//	} finally {
//		Objutil.closeQuietly(ois);
//		Objutil.closeQuietly(in);
//	}
//
//}
//
//private static byte[] toBytes(Object... array) {
//	if (array.length == 0)
//		return EMPTY_BYTES;
//	ByteArray baos = new ByteArray(512);
//	DataOutputStream out = new DataOutputStream(baos);
//	ObjectOutputStream oos = null;
//	try {
//		for (Object obj : array) {
//			if (obj == null) {
//				out.write(TYPE_NULL);
//				continue;
//			}
//			Byte b = PRIMITIVE_TYPES.get(obj.getClass());
//			if(b==null){
//				out.write(TYPE_SERIALIZABLE);
//				if (oos == null)
//					oos = new ObjectOutputStream(out);
//				oos.writeObject(obj);
//				oos.flush();
//				continue;
//			}
//			switch (b) {
//			case TYPE_BOOLEAN:
//				out.write(TYPE_BOOLEAN);
//				out.writeBoolean((Boolean) obj);
//				break;
//			case TYPE_BYTE:
//				out.write(TYPE_BYTE);
//				out.writeByte((Byte) obj);
//				break;
//			case TYPE_CHAR:
//				out.write(TYPE_CHAR);
//				out.writeChar((Character) obj);
//				break;
//			case TYPE_DOUBLE:
//				out.write(TYPE_DOUBLE);
//				out.writeDouble((Double) obj);
//				break;
//			case TYPE_FLOAT:
//				out.write(TYPE_FLOAT);
//				out.writeFloat((Float) obj);
//				break;
//			case TYPE_INT:
//				out.write(TYPE_INT);
//				out.writeInt((Integer) obj);
//				break;
//			case TYPE_LONG:
//				out.write(TYPE_LONG);
//				out.writeLong((Long) obj);
//				break;
//			case TYPE_SHORT:
//				out.write(TYPE_SHORT);
//				out.writeShort((Short) obj);
//				break;
//			case TYPE_STRING:
//				out.write(TYPE_STRING);
//				out.writeUTF((String) obj);
//				break;
//			case TYPE_BYTEARRAY:
//				out.write(TYPE_BYTEARRAY);
//				byte[] buf = (byte[]) obj;
//				out.writeInt(buf.length);
//				out.write(buf);
//				break;
//			default:
//				throw new IllegalArgumentException("type " + obj.getClass() + " is illegal");
//			}
//		}
//		return baos.toByteArray();
//	} catch (IOException e) {
//		throw new XutilRuntimeException(e);
//	} finally {
//		Objutil.closeQuietly(oos);
//		Objutil.closeQuietly(out);
//	}
//}
	public static Object read(DataInputStream in) throws IOException, ClassNotFoundException {
		switch (in.readByte()) {
		case TYPE_NULL:
			return null;
		case TYPE_BOOLEAN:
			return in.readBoolean();
		case TYPE_BYTE:
			return in.readByte();
		case TYPE_CHAR:
			return in.readChar();
		case TYPE_DOUBLE:
			return in.readDouble();
		case TYPE_FLOAT:
			return in.readFloat();
		case TYPE_INT:
			return in.readInt();
		case TYPE_LONG:
			return in.readLong();
		case TYPE_SHORT:
			return in.readShort();
		case TYPE_STRING:
			return in.readUTF();
		case TYPE_BYTEARRAY:
			byte[] buf = new byte[in.readInt()];
			in.readFully(buf);
			return buf;
		default:
			return new ObjectInputStream(in).readObject();
		}
	}

	public static void write(DataOutputStream out, Object obj) throws IOException {
		if (obj == null) {
			out.writeByte(TYPE_NULL);
			return;
		}
		Class type = obj.getClass();
		if (type == Boolean.class) {
			out.writeByte(TYPE_BOOLEAN);
			out.writeBoolean((Boolean) obj);
		} else if (type == Byte.class) {
			out.writeByte(TYPE_BYTE);
			out.writeByte((Byte) obj);
		} else if (type == Character.class) {
			out.writeByte(TYPE_CHAR);
			out.writeChar((Character) obj);
		} else if (type == Double.class) {
			out.writeByte(TYPE_DOUBLE);
			out.writeDouble((Double) obj);
		} else if (type == Float.class) {
			out.writeByte(TYPE_FLOAT);
			out.writeFloat((Float) obj);
		} else if (type == Integer.class) {
			out.writeByte(TYPE_INT);
			out.writeInt((Integer) obj);
		} else if (type == Long.class) {
			out.writeByte(TYPE_LONG);
			out.writeLong((Long) obj);
		} else if (type == Short.class) {
			out.writeByte(TYPE_SHORT);
			out.writeShort((Short) obj);
		} else if (type == String.class) {
			out.writeByte(TYPE_STRING);
			out.writeUTF((String) obj);
		} else if (type == byte[].class) {
			out.writeByte(TYPE_BYTEARRAY);
			byte[] buf = (byte[]) obj;
			out.writeInt(buf.length);
			out.write(buf);
		} else {
			out.writeByte(TYPE_SERIALIZABLE);
			new ObjectOutputStream(out).writeObject(obj);
		}
	}
	static Serializable NULL = new Serializable() {
		private static final long serialVersionUID = 1L;
		private Object readResolve() {
			return NULL;
		}
	};
	@Test
	public void testSerializeDeserialize() throws IOException {
		Logger.LOG.info("array name:{}", String[][][].class);
		byte[] bytes = serialize(NULL);
		Logger.LOG.info("len:{}, {}", bytes.length, bytes);
		assertSame(NULL, deserialize(bytes));
		String data = "mydata";
		Date time = new Date();
		assertEquals(data, deserialize(serialize(data)));
		assertEquals(time, deserialize(serialize(time)));
		assertSame(NULL, deserialize(serialize(NULL)));
		assertSame(DBType.db2, deserialize(serialize(DBType.db2)));
		Object[] params = new Object[] { NULL,1, 2L,NULL, 3.14d, 4.56f, 'c', (byte) 68, (short) 20000, true, null, "ddd",
				new java.util.Date(), new Object[] { "good", 1, null } };
		Event event = new Event("name", "value", params);
		// Event event2 = (Event) deserialize(serialize(event));

		Event event2 = new Event();
		ByteArray baos = new ByteArray(1024);
		DataOutputStream out = new DataOutputStream(baos);
		event.writeTo(out);
		event2.readFrom(new DataInputStream(baos.asInput()));
		assertEquals(event.getName(), event2.getName());
		assertEquals(event.getValue(), event2.getValue());
		assertArrayEquals(params, event2.parameters());

		System.out.println("length=" + bytes.length);

		System.out.println(event2.getName() + " " + event2.getValue() + " " + event2.parameters().length + " "
				+ event2.parameters());
		try{
		for(Object o : params){
			byte[] ret = Event.marshall(o);
			Logger.LOG.info("obj:{}  ,byte[]:{}", o,Arrays.toString(ret));
			if(o instanceof Object[])
				assertEquals((Object[])o, (Object[])Event.unmarshall(ByteArray.toStream(ret)));
			else
				assertEquals(o,Event.unmarshall(ByteArray.toStream(ret)));
		}
		}catch (Throwable  e) {
			throw Objutil.rethrow(e);
		}

		ByteArray ba = new ByteArray();
		ObjectOutputStream oos = new ObjectOutputStream(ba);
		Logger.LOG.info("len :{}",ba.size());
		oos.writeInt(1);
		Logger.LOG.info("len :{}",ba.size());
		oos.flush();
		Logger.LOG.info("len :{} {}",ba.size(),Arrays.toString(ba.toByteArray()));
	}

	@Test
	public void distributedCacheTest() {
		DMCache<String, User> primaryCache = new DMCache<String, User>(16, null, 1);
		Alias<String, User> cacheByUserName = primaryCache.createAlias(new Function<User, String>() {
			public String apply(User v) {
				return v.getName();
			}
		});
		User u = cacheByUserName.get("xiaozaichu");
		if (u == null) {
			u = new User("12345", "xiaozaichu");
			primaryCache.put("12345", u, true);
		}
		User u2 = cacheByUserName.get("xiaozaichu");
		assertEquals("xiaozaichu", u2.getName());
		assertEquals("12345", u2.getId());
		User u3 = primaryCache.get("12345");
		assertEquals("xiaozaichu", u3.getName());
		assertEquals("12345", u3.getId());
		assertSame(u2, u3);
		u2.setName("yuejie");
		primaryCache.put("12345", u2, true);

		u2 = cacheByUserName.get("xiaozaichu");
		assertTrue(null == u2);
		u2 = cacheByUserName.get("yuejie");
		assertTrue(u == u2);

		primaryCache.remove("12345", true);
		assertEquals(0, cacheByUserName.size());
		primaryCache.put("12345", u, true);
		long begin, end;
		begin = System.nanoTime();
		for (int i = 0; i < 100000; i++) {
			u2 = cacheByUserName.get("xiaozaichu");
			u3 = primaryCache.get("12345");
		}
		end = System.nanoTime();
		System.out.println("cache get nontime: " + (end - begin));
		assertTrue(null != primaryCache.get("12345"));
		try {
			Thread.sleep(1100); // timeout
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(null == primaryCache.get("12345"));
		assertEquals(0, cacheByUserName.size());
	}

	public static String str(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			if (b > 32)
				sb.append((char) b);
			else
				sb.append(b);
			sb.append(" ");
		}
		return sb.toString();
	}

	String test = "xzcxzc";

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeUTF(test);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		test = in.readUTF();
	}

	interface Aiface extends Function<String, Long> {
		void getbook(EventTest e, int i);

		Long apply(String s);
	}

	public static void main(String[] args) {
		new Event();
		System.out.println(deserialize(serialize(NULL)));
		byte[] bytes;

		EventTest et = new EventTest();
		bytes = serialize(et);
		System.out.println("length=" + bytes.length);
		System.out.println("et array=" + str(bytes));
		;

		bytes = serialize(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 });
		Object obj = deserialize(bytes);
		System.out.println("int[] " + obj.getClass());
		//
		// System.out.println(
		// "EventSaveObject//////////////////////////////////////");
		// Event event = new Event("rrrrrrr", "value");
		// bytes = serialize(event);
		// System.out.println("length=" + bytes.length);
		// System.out.println("eet array=" + str(bytes));
		// ;
		// event = (Event) deserialize(bytes);
		// System.out.println(event.getClass());
		//
		// System.out.println("//////////////////////////////////////");
		// event = new Event("hhhh", "value");
		// System.out.println(event.getName() + " " + event.getValue() + " " +
		// event.parameters());
		// bytes = serialize(event);
		// System.out.println("length=" + bytes.length);
		// System.out.println("array=" + str(bytes));
		// event = (Event) deserialize(bytes);
		// System.out.println(event.getName() + " " + event.getValue() + " " +
		// event.parameters());
		//
		// System.out.println("//////////////////////////////////////");
		//
		// event = new Event("name", "value", null,1, 2L, 3.14d, 4.56f, 'c',
		// (byte) 68, (short) 20000, true, null, "ddd",
		// new byte[] { 1, 2, -1 });
		// System.out.println(event.getName() + " " + event.getValue() + " " +
		// event.parameters());
		// bytes = serialize(event);
		// System.out.println("length=" + bytes.length);
		// System.out.println("array=" + str(bytes));
		// event = (Event) deserialize(bytes);
		// System.out.println(event.getName() + " " + event.getValue() + " " +
		// event.parameters());
		// System.out.println("//////////////////////////////////////");
		//
		// String[] datas = new String[] { "324", "dfg" };
		// event = new Event("name", "value", datas);
		// System.out.println(event.getName() + " " + event.getValue() + " " +
		// event.parameters().size() + " "
		// + event.parameters());
		// bytes = serialize(event);
		// System.out.println("length=" + bytes.length);
		// event = (Event) deserialize(bytes);
		// System.out.println(event.getName() + " " + event.getValue() + " " +
		// event.parameters().size() + " "
		// + event.parameters());

		Object[] params = new Object[] {1, 2L, 3.14d, 4.56f, 'c', NULL,(byte) 68, (short) 20000, true, null, "ddd",
				java.util.Date.class };// ,new Object[]{"good",1,null}};

		// Event event2 = (Event) deserialize(serialize(event));
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Object[]  l;
		long begin = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			Event event2 = new Event(), event = new Event("name", "value", params);
			ByteArray baos = new ByteArray(1024);
			DataOutputStream out = new DataOutputStream(baos);
			try {
				event.writeTo(out);
				event2.readFrom(new DataInputStream(baos.asInput()));
			} catch (IOException e) {
				throw new XutilRuntimeException(e);
			}
			l = event2.parameters();
		}
		long end = System.currentTimeMillis();
		System.out.println("time=" + (end - begin));
		ByteArray baos = new ByteArray();
		ObjectInputStream ois;
		ObjectOutputStream oos;
		InputStream in = baos.asInput();
		try {
			begin = System.currentTimeMillis();
			for (int i = 0; i < 100000; i++) {
				oos = new ObjectOutputStream(baos);
			}
			end = System.currentTimeMillis();
			System.out.println("ObjectOutputStream time=" + (end - begin));
//			begin = System.currentTimeMillis();
//			for (int i = 0; i < 100000; i++) {
//				ois = new ObjectInputStream(in);
//			}
//			end = System.currentTimeMillis();
//			System.out.println("ObjectInputStream time=" + (end - begin));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static byte[] serialize(Serializable obj) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(512);
		serialize(obj, out);
		return out.toByteArray();
	}

	public static void serialize(Serializable obj, OutputStream outputStream) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(outputStream);
			out.writeObject(obj);
			out.flush();
		} catch (IOException ex) {
			throw new XutilRuntimeException(ex);
		}
	}

	public static Object deserialize(byte[] data) {
		return deserialize(data, 0, data.length);
	}

	public static Object deserialize(byte[] data, int offset, int len) {
		try {
			return new ObjectInputStream(new ByteArrayInputStream(data, offset, len)).readObject();
		} catch (Exception e) {
			throw Objutil.rethrow(e);
		}
	}
}
