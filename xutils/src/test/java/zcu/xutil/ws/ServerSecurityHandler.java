/*
 * Copyright 2009 zaichu xiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zcu.xutil.ws;

import java.io.InputStream;
import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import zcu.xutil.Objutil;

import com.sun.xml.wss.ProcessingContext;
import com.sun.xml.wss.RealmAuthenticationAdapter;
import com.sun.xml.wss.SubjectAccessor;
import com.sun.xml.wss.XWSSProcessor;
import com.sun.xml.wss.XWSSProcessorFactory;
import com.sun.xml.wss.XWSSecurityException;
import com.sun.xml.wss.impl.misc.DefaultCallbackHandler;

/**
 * The Server side SecurityHandler.
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class ServerSecurityHandler implements SOAPHandler<SOAPMessageContext> {
	private static final String configFile = "META-INF/user-pass-authenticate-server.xml";

	private final XWSSProcessor processor;

	public ServerSecurityHandler() {
		InputStream config = Objutil.notNull(getClass().getClassLoader().getResourceAsStream(configFile), configFile);
		try {
			processor = XWSSProcessorFactory.newInstance().createProcessorForSecurityConfiguration(config,
					new DefaultCallbackHandler("server", null, new RealmAuthenticationAdapter() {
						@Override
						public boolean authenticate(Subject callerSubject, String username, String password)
								throws XWSSecurityException {
							if (username == null)
								return false;
							String s = getPassword(username);
							if (s == null || (s.length() > 0 && !s.equals(password)))
								return false;
							// populate the subject : ???
							// AccessController.doPrivileged ???
							String x500Name = "CN=" + username;
							Principal principal = new X500Principal(x500Name);
							callerSubject.getPrincipals().add(principal);
							if (!Objutil.isEmpty(password))
								callerSubject.getPrivateCredentials().add(password);
							return true;
						}
					}));
		} catch (Exception e) {
			throw Objutil.rethrow(e);
		} finally {
			Objutil.closeQuietly(config);
		}
	}

	protected String getPassword(String username) {
		return Objutil.systring("xutils.ws.user." + username);
	}

	@Override
	public final Set<QName> getHeaders() {
		return ClientSecurityHandler.securityQNames;
	}

	@Override
	public final boolean handleFault(SOAPMessageContext messageContext) {
		return true;
	}

	@Override
	public final void close(MessageContext messageContext) {// do nothing
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean handleMessage(SOAPMessageContext messageContext) {
		if (!(Boolean) messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
			try {
				SOAPMessage message = messageContext.getMessage();
				ProcessingContext context = processor.createProcessingContext(message);
				context.getExtraneousProperties().putAll(messageContext);
				context.setSOAPMessage(message);
				SOAPMessage verifiedMsg = processor.verifyInboundMessage(context);
				messageContext.setMessage(verifiedMsg);
				SubjectAccessor.setRequesterSubject(SubjectAccessor.getRequesterSubject(context));
			} catch (XWSSecurityException ex) {
				throw new WebServiceException(ex);
			}
		return true;
	}
}
