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
import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import zcu.xutil.Objutil;

import com.sun.xml.wss.ProcessingContext;
import com.sun.xml.wss.XWSSProcessor;
import com.sun.xml.wss.XWSSProcessorFactory;
import com.sun.xml.wss.XWSSecurityException;
import com.sun.xml.wss.impl.misc.DefaultCallbackHandler;

/**
 * The Class SecurityHandler.
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public final class ClientSecurityHandler implements SOAPHandler<SOAPMessageContext> {
	static final Set<QName> securityQNames = Collections
			.singleton(new QName(
					"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
					"Security", "wsse"));
	private static final String configFile = "META-INF/user-pass-authenticate-client.xml";
	private final XWSSProcessor processor;


	public ClientSecurityHandler() {
		InputStream config = Objutil.notNull(getClass().getClassLoader().getResourceAsStream(configFile), configFile);
		try {
			processor = XWSSProcessorFactory.newInstance().createProcessorForSecurityConfiguration(config,
					new DefaultCallbackHandler("client", null));
		} catch (Exception e) {
			throw  Objutil.rethrow(e);
		} finally {
			Objutil.closeQuietly(config);
		}
	}

	@Override
	public final Set<QName> getHeaders() {
		return securityQNames;
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
		if ((Boolean) messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))
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
		return true;
	}
}