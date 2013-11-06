package zcu.xutil.misc;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import zcu.xutil.Objutil;
import zcu.xutil.utils.ByteArray;
import zcu.xutil.web.Action;
import zcu.xutil.web.ActionContext;
import zcu.xutil.web.Stream;
import zcu.xutil.web.View;

public class CaptchaAction implements Action {
	static boolean verifyCaptcha(HttpServletRequest req, String captcha) {
		Object o;
		HttpSession s = req.getSession(false);
		if(s == null || ( o = s.getAttribute("xutils.web.captcha")) == null)
			return false;
		s.removeAttribute("xutils.web.captcha");
		return o.equals(captcha);
	}

	private final Random random = new Random();
	// dark grey--->light grey // orange
	private Color paintStart = new Color(60, 60, 60), paintEnd = new Color(140, 140, 140), textColor = new Color(255,
			153, 0);

	private String imageFormat;

	private int width = 150, height = 40, captchaLength = 6;

	@Override
	public View execute(ActionContext ac) throws ServletException, IOException {
		String captcha = randomNumber(captchaLength);
		BufferedImage image = captcha(captcha);
		if (imageFormat == null) {
			int i = ac.getActionName().lastIndexOf('.');
			imageFormat = i > 0 ? ac.getActionName().substring(i + 1) : "png";
		}
		ByteArray array = new ByteArray(32 * 1024);
		ImageIO.write(image, imageFormat, array);
		ac.getRequest().getSession().setAttribute("xutils.web.captcha", captcha);
		ac.getResponse().setContentType("image/" + imageFormat);
		return new Stream(array.getRawBuffer(), 0, array.size()).cache(0);
	}

	public void paintStartColor(int r, int g, int b) {
		paintStart = new Color(r, g, b);
	}

	public void paintEndColor(int r, int g, int b) {
		paintEnd = new Color(r, g, b);
	}

	public void textColor(int r, int g, int b) {
		textColor = new Color(r, g, b);
	}

	public void setImageFormat(String format) {
		imageFormat = format;
	}

	public void setCaptchaLength(int len) {
		Objutil.validate(len > 0 && len < 10, "captcha length: {}", len);
		captchaLength = len;
	}

	public void setWidth(int i) {
		width = i;
	}

	public void setHeight(int i) {
		height = i;
	}

	public String randomNumber(int length) {
		Objutil.validate(length > 0 && length < 10, "length > 0 && length <10");
		int tenth = (int) Math.pow(10, length - 1);
		return String.valueOf(tenth + random.nextInt(tenth * 9));
	}

	public BufferedImage captcha(String text) {
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bufferedImage.createGraphics();
		RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHints(rh);
		// dark grey--->light grey
		g2d.setPaint(new GradientPaint(0, 0, paintStart, 0, height / 2, paintEnd, true));
		g2d.fillRect(0, 0, width, height);
		g2d.setColor(textColor); // orange
		int fontsize = height / 2;
		char[] chars = text.toCharArray();
		int xGap = width / chars.length;
		g2d.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, fontsize));
		for (int i = 0; i < chars.length; i++)
			g2d.drawChars(chars, i, 1, i * xGap + random.nextInt(xGap / 2), fontsize + random.nextInt(fontsize));
		for (int i = 0, len = chars.length * 2; i < len; i++) {
			int p = random.nextInt(Integer.MAX_VALUE);
			int q = random.nextInt(Integer.MAX_VALUE);
			g2d.setColor(new Color(p % 255, q % 255, (p ^ q) % 255));
			g2d.drawLine(p % width, 0, q % width, height);
		}
		g2d.dispose();
		return bufferedImage;
	}
}
