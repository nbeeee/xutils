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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.URLDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import zcu.xutil.Objutil;
import zcu.xutil.XutilRuntimeException;

/**
 * 
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class MailHelper {

	/** pattern for extracting <img> tags */
	private static final Pattern pattern = Pattern
			.compile("(<[Ii][Mm][Gg]\\s*[^>]*?\\s+[Ss][Rr][Cc]\\s*=\\s*[\"'])([^\"']+?)([\"'])");

	private static final String MULTIPART_SUBTYPE_MIXED = "mixed";

	private static final String MULTIPART_SUBTYPE_RELATED = "related";

	private static final String MULTIPART_SUBTYPE_ALTERNATIVE = "alternative";

	private static final String CONTENT_TYPE_ALTERNATIVE = "text/alternative";

	private static final String HEADER_PRIORITY = "X-Priority";

	private static final String HEADER_CONTENT_ID = "Content-ID";

	private final MimeMessage mimeMessage;

	private MimeMultipart rootMimeMultipart;

	private MimeMultipart mimeMultipart;

	private String plaintext;
	private String htmltext;

	private HashMap<String, DataSource> inlines;
	private HashMap<String, DataSource> attachments;

	private boolean end;

	public MailHelper(Session session) {
		this.mimeMessage = new MimeMessage(session);
	}

	public final MimeMessage buildMimeMessage() throws MessagingException {
		if (end)
			return mimeMessage;
		if (htmltext != null) {
			ArrayList<File> imageList = new ArrayList<File>();
			htmltext = imageReplace(htmltext, imageList);
			int i = imageList.size();
			while (--i >= 0)
				addInline(String.valueOf(i), imageList.get(i));
			if (plaintext == null)
				getTextMimePart().setContent(htmltext, "text/html; charset=UTF-8");
			else {
				MimeMultipart messageBody = new MimeMultipart(MULTIPART_SUBTYPE_ALTERNATIVE);
				getMainPart().setContent(messageBody, CONTENT_TYPE_ALTERNATIVE);
				// Create the plain text part of the message.
				MimeBodyPart plainTextPart = new MimeBodyPart();
				plainTextPart.setText(plaintext, "UTF-8");
				messageBody.addBodyPart(plainTextPart);
				// Create the HTML text part of the message.
				MimeBodyPart htmlTextPart = new MimeBodyPart();
				htmlTextPart.setContent(htmltext, "text/html; charset=UTF-8");
				messageBody.addBodyPart(htmlTextPart);
				plaintext = null;
			}
			htmltext = null;
		} else if (plaintext != null) {
			getTextMimePart().setText(plaintext, "UTF-8");
			plaintext = null;
		}
		if (inlines != null) {
			for (Map.Entry<String, DataSource> e : inlines.entrySet()) {
				MimeBodyPart mimeBodyPart = new MimeBodyPart();
				mimeBodyPart.setDisposition(Part.INLINE);
				// We're using setHeader here to remain compatible with JavaMail
				// 1.2,
				// rather than JavaMail 1.3's setContentID.
				mimeBodyPart.setHeader(HEADER_CONTENT_ID, "<" + e.getKey() + ">");
				mimeBodyPart.setDataHandler(new DataHandler(e.getValue()));
				getMimeMultipart().addBodyPart(mimeBodyPart);
			}
			inlines = null;
		}
		if (attachments != null) {
			for (Map.Entry<String, DataSource> e : attachments.entrySet()) {
				MimeBodyPart mimeBodyPart = new MimeBodyPart();
				mimeBodyPart.setDisposition(Part.ATTACHMENT);
				try {
					mimeBodyPart.setFileName(MimeUtility.encodeWord(e.getKey(), "UTF-8", null));
				} catch (UnsupportedEncodingException ex) {// ignore

				}
				mimeBodyPart.setDataHandler(new DataHandler(e.getValue()));
				getRootMimeMultipart().addBodyPart(mimeBodyPart);
			}
			attachments = null;
		}
		end = true;
		return mimeMessage;
	}

	private MimePart getTextMimePart() throws MessagingException {
		return inlines != null || attachments != null ? getMainPart() : mimeMessage;
	}

	private final MimeMultipart getRootMimeMultipart() throws MessagingException {
		if (rootMimeMultipart == null)
			mimeMessage.setContent(rootMimeMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED));
		return rootMimeMultipart;
	}

	private final MimeMultipart getMimeMultipart() throws MessagingException {
		if (mimeMultipart == null) {
			MimeBodyPart relatedBodyPart = new MimeBodyPart();
			getRootMimeMultipart().addBodyPart(relatedBodyPart);
			relatedBodyPart.setContent(mimeMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED));
		}
		return mimeMultipart;
	}

	private MimeBodyPart getMainPart() throws MessagingException {
		MimeMultipart mainPart = getMimeMultipart();
		for (int i = 0, len = mainPart.getCount(); i < len; i++) {
			BodyPart bp = mainPart.getBodyPart(i);
			if (bp.getFileName() == null)
				return (MimeBodyPart) bp;
		}
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mainPart.addBodyPart(mimeBodyPart);
		return mimeBodyPart;
	}

	public void setFrom(String from, String personal) throws MessagingException {
		mimeMessage.setFrom(inetAddr(from, personal));
	}

	public void setReplyTo(String replyTo, String personal) throws MessagingException {
		mimeMessage.setReplyTo(new InternetAddress[] { inetAddr(replyTo, personal) });
	}

	public void addTo(String to, String personal) throws MessagingException {
		mimeMessage.addRecipient(Message.RecipientType.TO, inetAddr(to, personal));
	}

	public void addCc(String cc, String personal) throws MessagingException {
		mimeMessage.addRecipient(Message.RecipientType.CC, inetAddr(cc, personal));
	}

	public void addBcc(String bcc, String personal) throws MessagingException {
		mimeMessage.addRecipient(Message.RecipientType.BCC, inetAddr(bcc, personal));
	}

	public void setPriority(int priority) throws MessagingException {
		mimeMessage.setHeader(HEADER_PRIORITY, Integer.toString(priority));
	}

	public void setSentDate(Date sentDate) throws MessagingException {
		mimeMessage.setSentDate(Objutil.notNull(sentDate, "Sent date is null"));
	}

	public void setSubject(String subject) throws MessagingException {
		mimeMessage.setSubject(Objutil.notNull(subject, "Subject is null"), "UTF-8");
	}

	public void setText(String text) {
		plaintext = text;
	}

	public void setHtml(String html) {
		htmltext = html;
	}

	public void addInline(String contentId, DataSource dataSource) throws MessagingException {
		if (inlines == null)
			inlines = new HashMap<String, DataSource>();
		inlines.put(contentId, dataSource);
	}

	public void addInline(String contentId, File file) throws MessagingException {
		Objutil.validate(file.exists(), "cannot find the path: {}", file);
		addInline(contentId, new FileDataSource(file));
	}

	public void addInline(String contentId, URL url) throws MessagingException {
		addInline(contentId, new URLDataSource(url));
	}

	public void addAttachment(String attachmentFilename, DataSource dataSource) throws MessagingException {
		if (attachments == null)
			attachments = new HashMap<String, DataSource>();
		attachments.put(attachmentFilename, dataSource);
	}

	public void addAttachment(String attachmentFilename, File file) throws MessagingException {
		Objutil.validate(file.exists(), "cannot find the path: {}", file);
		addAttachment(attachmentFilename, new FileDataSource(file));
	}

	public void addAttachment(String attachmentFilename, URL url) throws MessagingException {
		addAttachment(attachmentFilename, new URLDataSource(url));
	}

	public static InternetAddress inetAddr(String addr, String personal) throws MessagingException {
		InternetAddress ret = new InternetAddress(addr);
		if (personal != null || (personal = ret.getPersonal()) != null)
			try {
				ret.setPersonal(personal, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new XutilRuntimeException(e);
			}
		return ret;
	}

	public static String imageReplace(String htmlMessage, List<File> imageList) {
		StringBuffer buffer = new StringBuffer(htmlMessage.length());
		// in the String, replace all "img src" with a CID and embed the related
		// image file if we find it.
		Matcher matcher = pattern.matcher(htmlMessage);
		// the matcher returns all instances one by one
		while (matcher.find()) {
			try {
				// in the RegEx we have the <src> element as second "group"
				File file = new File(matcher.group(2)).getCanonicalFile();
				int i = imageList.indexOf(file);
				if (i < 0) {
					i = imageList.size();
					imageList.add(file);
				}
				// if we embedded something, then we need to replace the URL
				// with
				// the CID, otherwise the Matcher takes care of adding the
				// non-replaced text afterwards, so no else is necessary here!
				matcher.appendReplacement(buffer, matcher.group(1) + "cid:" + i + matcher.group(3));
			} catch (IOException e) {
				throw new XutilRuntimeException(e);
			}
		}
		// append the remaining items...
		matcher.appendTail(buffer);
		return buffer.toString();
	}
}
