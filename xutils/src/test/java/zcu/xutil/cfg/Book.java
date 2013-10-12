package zcu.xutil.cfg;

import zcu.xutil.Disposable;
import zcu.xutil.FirstBean;
import zcu.xutil.Logger;
import zcu.xutil.Objutil;


public class Book implements Disposable,Cloneable{
	final String name;
	double price;
	int number;
	public String str;
	public Book(){
		name=" dddd";
	}

	public Book(String name, double price, int number) {
		this.name = name;
		this.price = price;
		this.number = number;
	}
	public void setStr(String str){
		this.str=str;
	}
	public String toString(){
		return new zcu.xutil.ToStrBuilder(Book.class)
		 	.append( "name",this.name)
		 	.append( "price",this.price)
		 	.append( "number",this.number)
		    .toString();
	}
@Override
protected Object clone() throws CloneNotSupportedException {
	Object ret=super.clone();
	System.out.println("============call book,clone");
	return ret;
}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + number;
		long temp;
		temp = Double.doubleToLongBits(price);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Book))
			return false;
		final Book other = (Book) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (number != other.number)
			return false;
		if (Double.doubleToLongBits(price) != Double.doubleToLongBits(other.price))
			return false;
		return true;
	}

	public void destroy() {
		Logger.LOG.info("destroy Book: {}",this);

	}
}
