package zcu.xutil.cfg;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import zcu.xutil.Logger;
import zcu.xutil.txm.Transactional;
import zcu.xutil.txm.TxManager;
import zcu.xutil.utils.Checker;
import zcu.xutil.utils.Matcher;



public class Store implements TestService{
	private static final Logger logger = Logger.getLogger(Store.class);
	static String logo="logo";
	String storeName;
	Book[] books;
	List<Book> booklist;
	Map<String,Book> bookmap;
	Set<Book> bookset;

	public Store(){
	}

	public Store(String name,Book...books){
		storeName=name;
		this.books=books;
	}
	public Store(String name,String list,List<Book> booklist){
		storeName=name;
		this.booklist=booklist;
		System.out.println(list+booklist);
	}
	public Store(Map<String,Book> bookmap){
		this.bookmap=bookmap;
		System.out.println("Map: "+bookmap);
	}
	public Store(String name,String map,String set,Set<Book> bookset){
		storeName=name;
		this.bookset=bookset;
		System.out.println(map+set+bookset);
	}


	public String toString(){
		return new zcu.xutil.ToStrBuilder(Store.class)
		 	.append( "storeName",this.storeName)
		 	.append( "books",Arrays.toString(this.books))
		 	.append( "booklist",this.booklist)
		 	.append( "bookmap",this.bookmap)
		 	.append( "bookset",this.bookset)
		 	.append("logo", logo)
		    .toString();
	}
	public String echo(String str) {
		String result = TxManager.existActivTx() ? str+ " inTx" : str+ " noTx";
		logger.info("!!!!!!!!!!" +result);
		return result;
	}
	public String no(String str) {
		String result = TxManager.existActivTx() ? str+ " inTx" : str+ " noTx";
		logger.info("!!!!!!!!!!" +result);
		return result;
	}
	public void start() {
		logger.info("store started");

	}
	interface B extends TestService{

		String rrrr(String str);
	}
	static class Anno implements Comparable<Anno> ,B{

		public int compareTo(Anno o) {
			// TODO Auto-generated method stub
			return 0;
		}

		public String rrrr(String str) {
			// TODO Auto-generated method stub
			return null;
		}

		public String echo(String str) {
			// TODO Auto-generated method stub
			return null;
		}

		public String no(String str) {
			// TODO Auto-generated method stub
			return null;
		}

	}
	public static void main(String[] args) {
		for(Constructor ct : Store.class.getDeclaredConstructors()){
			System.out.println(ct);
		}
		for(Annotation a : Store.class.getAnnotations() )
			System.out.println(a);
		for(Method m : B.class.getMethods() ){
			System.out.println(m);
			for(Annotation a : m.getAnnotations() )
				System.out.println(a);
		}
		Checker<Class> match = Matcher.annoInherit(Transactional.class);
		System.out.println(match.checks(Store.class));
		System.out.println(match.checks(Anno.class));
	}

}
