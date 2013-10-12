package zcu.xutil.web;

import static javax.servlet.http.HttpServletResponse.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


import javax.servlet.ServletException;


public class ResumeAction implements Action {
	private File rootdir;
	public String filename;
	@Validator(value = "(?i)true|false", message = "resumeAction.inline")
	public boolean inline;

	public void setRootDir(File directory) {
		this.rootdir = directory;
	}

	public File getRootDir() {
		return rootdir;
	}

	public View listSubDir(ActionContext ac, File rootDir, File subDir) {
		ArrayList<File> files = new ArrayList<File>();
		if (subDir.getPath().length() > rootDir.getPath().length())
			files.add(new File(subDir, ".."));
		for (File item : subDir.listFiles()) {
			if (!item.isHidden() && item.canRead())
				files.add(item);
		}
		return new Forward(ac.getActionName() + ".mvel").add("files", files);
	}

	public View execute(ActionContext ac) throws ServletException {
		try {
			if (rootdir == null || !rootdir.exists()) {
				ac.getResponse().sendError(SC_FORBIDDEN, "can't access.");
				return null;
			}
			File file = rootdir;
			if (filename != null) {
				file = new File(filename);
				if (!file.isAbsolute())
					file = new File(rootdir, filename);
				if (!file.exists()) {
					ac.getResponse().sendError(SC_NOT_FOUND, filename + ": file not find.");
					return null;
				}
				file = file.getCanonicalFile();
			}
			if (!file.getPath().startsWith(rootdir.getPath())) {
				ac.getResponse().sendError(SC_FORBIDDEN, "can't access.");
				return null;
			}
			if (file.isDirectory())
				return listSubDir(ac, rootdir, file);
			return new Stream(file).attachment();
		} catch (IOException e) {
			throw new ServletException(e);
		}
	}
//	private void sendPDF(HttpServletRequest req, HttpServletResponse resp, String fullDoc) throws IOException {
//		long fByte;
//		long lByte;
//		StringTokenizer stok;
//		String tok;
//		String curRange;
//		File ckFile = new File(fullDoc);
//		String shortFile = fullDoc.substring(fullDoc.lastIndexOf(File.separator) + 1);
//		resp.setHeader("Content-Disposition", "inline;filename=\"" + shortFile + "\"");
//
//		String rangeStr = req.getHeader("Range");
//		long contentLength = 0;
//
//		// check to see if there was a range specified, First time in from the
//		// request.
//		if (rangeStr == null) {
//			resp.setContentLength((int) (new File(fullDoc)).length());
//			resp.setHeader("Accept-ranges", "bytes");
//			FileInputStream fis = new FileInputStream(fullDoc);
//			BufferedOutputStream out = new BufferedOutputStream(resp.getOutputStream());
//
//			byte[] inputLine = new byte[1024];
//			int numRead = 0;
//			int byteCounter = 0;
//			resp.setContentType("application/pdf");
//			shortFile = fullDoc.substring(fullDoc.lastIndexOf(File.separator) + 1);
//			try {
//				while ((numRead = fis.read(inputLine)) != -1) {
//					byteCounter = numRead;
//					out.write(inputLine, 0, numRead);
//					for (int y = 0; y < 10000; y++)
//						;
//				}
//				fis.close();
//				out.close();
//			} catch (SocketException se) {
//				fis.close();
//			}
//		} else {
//			// remove the "bytes=" off the front of the range string
//			rangeStr = rangeStr.substring(rangeStr.indexOf("=") + 1);
//			// now divide up the ranges into their groups
//			StringTokenizer ranges = new StringTokenizer(rangeStr, ",");
//
//			long fileSize = (new File(fullDoc)).length();
//			// go through and verify the ranges are valid
//			while (ranges.hasMoreTokens()) {
//				// get the next range set
//				curRange = ranges.nextToken();
//				// breakup the range set
//				stok = new StringTokenizer(curRange, " ,-");
//				tok = stok.nextToken();
//				// convert the first range to a long value
//				fByte = (new Integer(tok)).longValue();
//				lByte = fileSize;
//				// if there is a second value, convert it too
//				if (stok.hasMoreTokens()) {
//					lByte = (new Integer(stok.nextToken())).longValue();
//				}
//				// test to make sure the ranges are valid
//				if (fByte < 0 || fByte > lByte || lByte > fileSize) {
//					// error in range
//					return;
//				}
//				contentLength = lByte - fByte + 1;
//			}
//			// the ranges are valid, since we pulled out all of the tokens
//			// to check the ranges we need to tokenize again
//			// now divide up the ranges into their groups
//			ranges = new StringTokenizer(rangeStr, ",");
//
//			// set some header stuff
//
//			String boundary = "multipart";
//
//			resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
//			resp.setHeader("Accept-ranges", "bytes");
//			resp.setHeader("Content-Length", String.valueOf(contentLength));
//			resp.setHeader("Content-Range", rangeStr);
//			resp.setContentType("multipart/x-byteranges; boundary=", boundary);
//
//			// open the output stream
//			ServletOutputStream out = resp.getOutputStream();
//			out.flush();
//			while (ranges.hasMoreTokens()) {
//				curRange = (String) ranges.nextElement();
//				stok = new StringTokenizer(curRange, " ,-");
//				long fTok = (new Integer(stok.nextToken())).longValue();
//				if (stok.hasMoreTokens()) {
//					fByte = fTok;
//					lByte = (new Integer(stok.nextToken())).longValue();
//				} else {
//					if (curRange.startsWith("-")) {
//						fByte = fileSize - fTok;
//						lByte = fileSize;
//					} else {
//						fByte = 0;
//						lByte = fTok;
//					}
//				}
//				long bytesToRead = lByte - fByte + 1;
//
//				out.println();
//				out.println("--" + boundary);
//				out.println("Content-type: application/pdf");
//				out.println("Content-Range: bytes " + fByte + "-" + lByte + "/" + fileSize);
//				out.println();
//
//				// open the file and send it!
//				FileInputStream fis = new FileInputStream(fullDoc);
//				fis.skip(fByte);
//				byte[] inputLine = new byte[1024];
//				int bytesRead = 0;
//				try {
//					while (bytesToRead > 0) {
//						if (bytesToRead < inputLine.length) {
//							bytesRead = fis.read(inputLine, 0, (int) bytesToRead);
//						} else {
//							bytesRead = fis.read(inputLine);
//						}
//						if (bytesRead < 0) {
//							break;
//						}
//						bytesToRead -= bytesRead;
//						// write the bytes out to the browser
//						out.write(inputLine, 0, bytesRead);
//						out.flush();
//					}
//					if (bytesToRead != 0)
//						System.out.println("bytesToRead is not zero: " + bytesToRead);
//					fis.close();
//					out.flush();
//				} catch (SocketException se) {
//					fis.close();
//				}
//
//			}
//			// close the current boundary
//			out.println();
//			out.println("" + boundary + "");
//			// close the output stream
//			out.close();
//		}
//	}
}
