package zcu.xutil.txm.test;

import java.sql.SQLException;

import zcu.xutil.sql.Query;



public class EntityDaoImpl implements EntityDao {
	Query query;
	public EntityDaoImpl(Query _query){
		query=_query;
	}


	public User create(User user) throws SQLException {
		int ret=query.entityUpdate(User.create, user);
		if(ret!=1)
			throw new SQLException("return value: "+ret);
		return user;
	}
	public User update(User user) throws SQLException{
		int ret=query.entityUpdate(User.update, user);
		if(ret!=1)
			throw new SQLException("return value: "+ret);
		return user;
	}
}
