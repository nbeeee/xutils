package zcu.xutil.cfg;

import zcu.xutil.txm.Propagation;
import zcu.xutil.txm.Transactional;
@Transactional(Propagation.SUPPORTS)
public interface TestService {
	@Transactional
	String echo(String str);
	String no(String str);
}
