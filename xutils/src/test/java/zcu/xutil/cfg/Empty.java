package zcu.xutil.cfg;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import zcu.xutil.Logger;
import zcu.xutil.Objutil;

public class Empty implements TestService {
	public Empty() {

	}

	public Empty(Object object) {
		System.out.println("Object: " + object);
	}

	public Empty(String object) {
		System.out.println("String: " + object);
	}

	public Empty(TestService ts) {
		System.out.println("TestServicee: " + ts);
	}

	public Empty(TestService ts, java.sql.Date date) {
		System.out.println("TestService java.sql.Date");

	}

	public Empty(Store ts, java.util.Date date) {
		System.out.println("Store java.util.Date");
	}

	public String echo(String str) {
		// TODO Auto-generated method stub
		return str;
	}

	public String no(String str) {
		// TODO Auto-generated method stub
		return str;
	}

	@Override
	public String toString() {
		return "Empty TestService";
	}

	public static class EmptySUB extends Empty {
		public String echo(String str) {
			// TODO Auto-generated method stub
			return str;
		}

		public String no(String str) {
			// TODO Auto-generated method stub
			return str;
		}

		public static void main(String[] args) throws IOException, SAXException {
			//
		}
	}

}
