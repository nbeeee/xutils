package zcu.xutil.txm.test;

import java.sql.SQLException;

import zcu.xutil.txm.Propagation;
import zcu.xutil.txm.Transactional;



@Transactional(rollbackfor={SQLException.class})
public interface EntityDao {
	User create(User user)throws SQLException;
	@Transactional(Propagation.NOT_SUPPORTED)
	User update(User user)throws SQLException;
}
