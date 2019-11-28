package utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.Part;

public class MultipartInputStream extends InputStream
{
	private Collection<Part> parts;
	private InputStream currentPartInput;
	private Part currentPart;
	private ByteArrayInputStream currentPartHeader;
	private ByteArrayInputStream footer;
	private Iterator<Part> partIt;
	private final String boundary;
	
	public MultipartInputStream(Collection<Part> parts)
	{
		this.parts = parts;
		partIt = this.parts.iterator();
		
		boundary = "===" + System.currentTimeMillis() + "===";
		footer = new ByteArrayInputStream(getFooter());
		
		try
		{
			getNextPart();
		}
		catch(IOException e)
		{
		}
	}
	
	public String getBoundary()
	{
		return boundary;
	}
	
	public int read() throws IOException
	{
		if(currentPart != null)
		{
			if(currentPartHeader.available() > 0)
			{
				return currentPartHeader.read();
			}
			
			if(currentPartInput.available() > 0)
			{
				return currentPartInput.read();
			}
			
			getNextPart();
			if(currentPart != null)
			{
				return read();
			}
		}
		
		if(footer.available() > 0)
		{
			return footer.read();
		}
		
		return -1;
	}
	
	public int read(byte[] b, int off, int len) throws IOException
	{
		if(b.length == 0)
			return 0;
		
		int globalBytesRead = 0;
		
		while(len > 0 && currentPart != null)
		{
			if(currentPartHeader.available() > 0)
			{
				int bytesRead = currentPartHeader.read(b, off, len);
				len -= bytesRead;
				off += bytesRead;
				globalBytesRead += bytesRead;
			}
			if(len > 0 && currentPartInput.available() > 0)
			{
				int bytesRead = currentPartInput.read(b, off, len);
				len -= bytesRead;
				off += bytesRead;
				globalBytesRead += bytesRead;
			}
			if(len > 0)
			{
				getNextPart();
			}
		}
		
		if(len > 0 && footer.available() > 0)
		{
			globalBytesRead += footer.read(b, off, len);
		}
		
		return globalBytesRead == 0 ? -1 : globalBytesRead;
	}
	
	public void close() throws IOException
	{
		closeCurrentPart();
		while(partIt.hasNext())
		{
			var part = partIt.next();
			part.getInputStream().close();
			part.delete();
		}
		footer.close();
	}
	
	public int available() throws IOException
	{
		var avail = footer.available();
		
		if(currentPart != null)
		{
			avail += currentPartHeader.available() + currentPartInput.available();
		}
		
		return avail;
	}
	
	private void getNextPart() throws IOException
	{
		if(partIt.hasNext())
		{
			closeCurrentPart();
			currentPart = partIt.next();
			currentPartInput = currentPart.getInputStream();
			currentPartHeader = new ByteArrayInputStream(getPartHeader());
		}
		else
		{
			currentPart = null;
		}
	}
	
	private byte[] getPartHeader()
	{
		if(currentPart != null)
		{
			return String.format("\r\n--%s\r\nContent-Disposition: %s\r\n\r\n", boundary, currentPart.getHeader("Content-Disposition")).getBytes();
		}
		
		return new byte[] {};
	}
	
	private byte[] getFooter()
	{
		return String.format("\r\n--%s--", boundary).getBytes();
	}
	
	private void closeCurrentPart() throws IOException
	{
		if(currentPart != null)
		{
			currentPartInput.close();
			currentPartHeader.close();
			currentPart.delete();
		}
	}
}
