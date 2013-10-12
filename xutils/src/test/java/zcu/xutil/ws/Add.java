package zcu.xutil.ws;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.security.auth.Subject;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import com.sun.xml.wss.SubjectAccessor;
import com.sun.xml.wss.XWSSecurityException;

@WebService(endpointInterface = "zcu.xutil.ws.IAdd")
public class Add implements IAdd {
	@Resource
	WebServiceContext context;

	public int add(int a, int b) {
		MessageContext ctx = context.getMessageContext();
		Subject clientSubject=null;
		try {
			System.out.println(context.getUserPrincipal());
			clientSubject = SubjectAccessor.getRequesterSubject(context);
			System.out.println("clientSubject:"+clientSubject);
			clientSubject = SubjectAccessor.getRequesterSubject(ctx);
			System.out.println("ctx:"+clientSubject);
			System.out.println("SubjectAccessor:"+SubjectAccessor.getRequesterSubject());
		} catch (XWSSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String username = null;
		if (clientSubject != null) {
			Set<Principal> principals = clientSubject.getPrincipals();
			for (Principal principal : principals) {
				username=principal.getName();
				if(username!=null)
					break;
			}
			System.out.println("username:"+username);
		} else {
			System.out.println("Client Principal not set");
			// return from here
		}


		System.out.println("WebServiceContext:" + context.getClass());
		System.out.println("context:" + ctx);
		Map<String, List<String>> obj = (Map<String, List<String>>) ctx.get(MessageContext.HTTP_REQUEST_HEADERS);
		System.out.println("reqHeaders:" + obj);
		for (Entry<String, List<String>> entry : obj.entrySet()) {
			System.out.println("key: " + entry.getKey() + "  value: " + entry.getValue());
		}

		return a + b;
	}
}
