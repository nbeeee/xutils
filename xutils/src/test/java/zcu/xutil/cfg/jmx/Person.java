package zcu.xutil.cfg.jmx;

import zcu.xutil.jmx.MbeanAttribute;
import zcu.xutil.jmx.MbeanOperation;
import zcu.xutil.jmx.MbeanResource;

@MbeanResource
public class Person {
	private volatile int age;
	private String[] name;
	private boolean man;

	public Person(){
		//no
	}
	public Person(int age, String[] name, boolean man) {
		super();
		this.age = age;
		this.name = name;
		this.man = man;
	}
	@MbeanAttribute(mutable=true)
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	@MbeanAttribute(mutable=true)
	public boolean isMan() {
		return man;
	}
	public void setMan(boolean man) {
		this.man = man;
	}
	@MbeanAttribute(mutable=true)
	public String[] getName() {
		return name;
	}
	public void setName(String[]name) {
		this.name = name;
	}
	@MbeanOperation(description="fix xiao age")
	public int updateAge(int _age){
		age=_age;
		return age;
	}
}
