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
package zcu.xutil.misc;

import java.io.File;
import java.util.Date;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class MailSender {
	private Properties props;
	private Session session;
	private String password;

	/**
	 * properties <code>
	 * mail.password=OBF:KHxEr[
	 * mail.user=user
	 * mail.from=user@host
	 * mail.host=host
	 * mail.smtp.starttls.enable=true|false
	 * mail.smtp.auth=true|false
	 * mail.[stmp|pop3|imap].host=smtp.yeepay.com|pop3.yeepay.com
	 * mail.transport.protocol=smtp
	 * mail.store.protocol=imap|pop3
	 * </code>
	 * 
	 */
	public synchronized void setProperties(Properties properties) {
		if (session == null) {
			String pwd = properties.getProperty("mail.password");
			if (pwd != null)
				setPassword(pwd);
			this.props = properties;
		}
	}

	public void setPassword(String pwd) {
		password = pwd.startsWith("OBF:") ? Objutil.unobfuscate(pwd.substring(4)) : pwd;
	}

	public synchronized Session getSession() {
		if (session == null) {
			session = (Session.getInstance(props));
			props = null;
		}
		return session;
	}

	public synchronized void setSession(Session s) {
		session = Objutil.notNull(s, "session is null");
		props = null;
	}

	public void send(Preparator preparator) {
		try {
			MimeMessage message = preparator.prepare(getSession());
			if (message.getSentDate() == null)
				message.setSentDate(new Date());
			String messageId = message.getMessageID();
			message.saveChanges();
			if (messageId != null) // Preserve explicitly specified message
									// id...
				message.setHeader("Message-ID", messageId);
			Transport transport = getSession().getTransport();
			try {
				transport.connect(null, password);
				transport.sendMessage(message, message.getAllRecipients());
			} finally {
				transport.close();
			}
		} catch (MessagingException e) {
			throw new XutilRuntimeException(e);
		}
	}

	public final void send(String text, String subject, String to) {
		send(text, subject, to, null);
	}

	public final void send(String text, String subject, String to, String cc) {
		send(text, subject, to, cc, null);
	}

	public void send(final String text, final String subject, final String to, final String cc, final String from,
			final File... attachments) {
		send(new Preparator() {
			@Override
			public MimeMessage prepare(Session s) throws MessagingException {
				MailHelper h = new MailHelper(s);
				if (!Objutil.isEmpty(from))
					h.setFrom(from, null);
				if (!Objutil.isEmpty(subject))
					h.setSubject(subject);
				for (String str : Objutil.split(to, ';'))
					h.addTo(str, null);
				for (String str : Objutil.split(cc, ';'))
					h.addCc(str, null);
				if (text.trim().startsWith("<html"))
					h.setHtml(text);
				else
					h.setText(text);
				for (File file : attachments)
					h.addAttachment(file.getName(), file);
				return h.buildMimeMessage();
			}
		});
	}
}
