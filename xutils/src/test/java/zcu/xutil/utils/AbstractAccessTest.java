package zcu.xutil.utils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import zcu.xutil.FirstBean;
import zcu.xutil.Objutil;
import zcu.xutil.utils.Checker;

public class AbstractAccessTest {
	public static @Inject("f1")
	String field1;
	public static String field2;

	public @Inject("f3")
	String field3;
	public String field4;

	public static class B extends AbstractAccessTest {
		public static @Inject("f5")
		String field5;
		public static String field6;

		public @Inject("f7")
		String field7;
		public String field8;
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testField() {
		AccessorCache access1 = AccessorCache.field(32, null, false);
		AccessorCache access2 = AccessorCache.field(32, null, true);
		Map<String, Accessor> list = access1.getAllAccessor(B.class);
		assertEquals(4, list.size());
		list = access2.getAllAccessor(B.class);
		assertEquals(8, list.size());
	}

	@Test
	public void testFieldFilter() {
		AccessorCache access = AccessorCache.field(32, new Checker<Accessor>() {
			public boolean checks(Accessor accessor) {
				Inject inject = accessor.getAnnotation(Inject.class);
				if (inject == null || Objutil.isEmpty(inject.value()))
					return true;
				return false;
			}
		}, true);
		Map<String, Accessor> list = access.getAllAccessor(B.class);
		assertEquals(4, list.size());
		assertTrue(list.containsKey("field1"));
		assertTrue(list.containsKey("field3"));
		assertTrue(list.containsKey("field5"));
		assertTrue(list.containsKey("field7"));
		Map<String, Accessor> list2 = access.getAllAccessor(B.class);
		assertSame(list, list2);
		list = access.getAllAccessor(AbstractAccessTest.class);
		assertEquals(2, list.size());
		assertTrue(list.containsKey("field1"));
		assertTrue(list.containsKey("field3"));
	}

	@Test
	public void testAccessorFill() {
		// AbstractAccess access = new AccessProperty(32,new
		// Checker<Accessor>(){
		// public boolean checks(Accessor accessor) {
		// return accessor.getName().equals("class");
		// }});
		AccessorCache access = AccessorCache.property(32, null);
		Map m = access.getAllAccessor(ArrayList.class);

		System.out.println("arraylist:" + m.toString());

		FirstBean first = new FirstBean();
		first.setFirstString("firstString");
		first.setFirstInteger(new Integer(95));
		first.setFirstInt(100);
		byte[] bytes = new byte[] { 1, 2, 3, 4 };
		first.setFirstbytes(bytes);
		FirstBean to = new FirstBean();
		fill(access, to, first, null);
		assertEquals(to.getFirstString(), first.getFirstString());
		assertEquals(to.getFirstInteger(), first.getFirstInteger());
		assertEquals(to.getFirstInt(), first.getFirstInt());
		assertEquals(to.getFirstbytes(), first.getFirstbytes());

		for (String acs : access.getAllAccessor(FirstBean.class).keySet())
			System.out.println(acs);

	}

	public static void fill(AccessorCache c, Object to, Object from, Map<String, String> toFrom) {
		Map<String, Accessor> getters = c.getAllAccessor(from.getClass());
		System.out.println("from: " + getters.toString());
		for (Map.Entry<String, Accessor> setter : c.getAllAccessor(to.getClass()).entrySet()) {
			System.out.println("setter name: " + setter.getValue().getName());

			String source = toFrom == null ? null : toFrom.get(setter.getKey());
			Accessor getter = getters.get(source == null ? setter.getKey() : source);
			if (getter != null)
				setter.getValue().setValue(to, getter.getValue(from));

		}
	}
}
