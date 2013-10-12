package zcu.xutil.sql;


import zcu.xutil.sql.ID;
import zcu.xutil.sql.NpSQL;
import zcu.xutil.sql.ResultHandler;
import zcu.xutil.sql.handl.BeanRow;

public class IdentityEntity {
	public Long id;
	public String eventName;

	public IdentityEntity(){
		//no
	}

	public IdentityEntity(String _eventName){
		eventName=_eventName;
	}

	public String getEventName() {
		return eventName;
	}
	public void setEventName(String _eventName) {
		this.eventName = _eventName;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long _id) {
		this.id = _id;
	}

	public String toString(){
		return new zcu.xutil.ToStrBuilder(IdentityEntity.class)
	 	.append( "id",id)
	 	.append( "eventName",eventName)
	    .toString();
	}
	public final static ResultHandler<IdentityEntity> handler= new BeanRow(IdentityEntity.class);
	public final static	String droptable="DROP TABLE t_event";
	//public final static	String createtable="CREATE TABLE t_event (id int generated always as identity,eventname VARCHAR(50),PRIMARY KEY(id))";
	public final static	String createtable="CREATE TABLE t_event (id int IDENTITY,eventname VARCHAR(50),PRIMARY KEY(id))";

	public final static	String retrieve = "select * from t_event where id=?";
	public final static	NpSQL  create =new NpSQL(new ID("id").ide(DBType.h2.identitySQL("")),
			"insert into t_event (eventname) values(:eventName)");
	public final static	NpSQL  update = new NpSQL(
			"update t_event set eventname=:eventName where id=:id");
	public final static	String delete ="delete from t_event where id=?";



}
