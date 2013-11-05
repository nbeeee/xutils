package zcu.xutil.msg;

import java.util.Collection;
import org.jgroups.Address;

public interface Server {
	Address getAddress();
	int getVerison();
	Collection<String> getServices();
}
