package zcu.xutil.txm.test;

import zcu.xutil.sql.Handler;
import zcu.xutil.sql.NpSQL;
import zcu.xutil.sql.ResultHandler;
import zcu.xutil.sql.handl.BeanRow;



public class User{

	private String id;
	private String name;

	public User(){
		//
	}

	public User(String _id,String _name){
		this.id=_id;
		this.name=_name;
	}
	public String getId(){
		return id;
	}
	public void setId(String _id) {
		this.id = _id;
	}
	public String getName(){
		return name;
	}
	public void setName( String _name){
		this.name=_name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof User))
			return false;
		final User other = (User) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public String toString(){
		return new zcu.xutil.ToStrBuilder(User.class)
	 	.append( "id",id)
	 	.append( "name",name)
	    .toString();
	}
	public final static ResultHandler<User> handler= new BeanRow<User>(User.class);
	public final static	String droptable="DROP TABLE t_user";
	public final static	String createtable="CREATE TABLE t_user (id VARCHAR(30) NOT NULL,name VARCHAR(28) NOT NULL,PRIMARY KEY(id))";
	public final static	String retrieve = "select * from t_user where id=?";
	public final static	String delete ="delete from t_user where id=?";

	public final static	NpSQL create = new NpSQL(
			"insert into t_user values(:id,:name)");
	public final static	NpSQL update = new NpSQL(
			"update t_user set name=:name where id=:id");
}
