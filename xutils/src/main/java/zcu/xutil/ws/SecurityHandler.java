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
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.sun.xml.wss.ProcessingContext;
import com.sun.xml.wss.RealmAuthenticationAdapter;
import com.sun.xml.wss.SubjectAccessor;
import com.sun.xml.wss.XWSSProcessor;
import com.sun.xml.wss.XWSSProcessorFactory;
import com.sun.xml.wss.XWSSecurityException;
import com.sun.xml.wss.impl.misc.DefaultCallbackHandler;

/**
 * The Class SecurityHandler.
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class SecurityHandler implements SOAPHandler<SOAPMessageContext> {

	private static CallbackHandler serverHandler(RealmAuthenticationAdapter authenticator) {
		try {
			return new DefaultCallbackHandler("server", null, authenticator);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private final boolean isClient;
	private final XWSSProcessor processor;

	/**
	 * Instantiates a new client security handler.
	 *
	 *
	 */
	public SecurityHandler() {
		this(true, null);
	}

	/**
	 * Instantiates a new servet security handler.
	 *
	 *
	 */
	public SecurityHandler(RealmAuthenticationAdapter authenticator) {
		this(false, serverHandler(authenticator));
	}

	public SecurityHandler(boolean client, CallbackHandler callback) {
		this.isClient = client;
		String s = client ? "META-INF/user-pass-authenticate-client.xml" : "META-INF/user-pass-authenticate-server.xml";
		InputStream config = getClass().getClassLoader().getResourceAsStream(s);
		if (config == null)
			throw new IllegalArgumentException("not found config:".concat(s));
		try {
			if (callback == null)
				callback = client ? new DefaultCallbackHandler("client", null) : serverHandler(null);
			XWSSProcessorFactory factory = XWSSProcessorFactory.newInstance();
			this.processor = factory.createProcessorForSecurityConfiguration(config, callback);
		} catch (XWSSecurityException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				config.close();
			} catch (Exception e) {//

			}
		}
	}
	@Override
	public final Set<QName> getHeaders() {
		QName securityHeader = new QName(
				"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security", "wsse");
		HashSet<QName> headers = new HashSet<QName>();
		headers.add(securityHeader);
		return headers;
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
		boolean out = (Boolean) messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		if (isClient) {
			if (out)
				try {
					SOAPMessage message = messageContext.getMessage();
					ProcessingContext context = processor.createProcessingContext(message);
					context.getExtraneousProperties().putAll(messageContext);
					context.setSOAPMessage(message);
					SOAPMessage secureMsg = processor.secureOutboundMessage(context);
					messageContext.setMessage(secureMsg);
				} catch (XWSSecurityException ex) {
					throw new RuntimeException(ex);
				}
		} else if (!out)
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
