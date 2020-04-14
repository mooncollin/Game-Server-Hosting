package utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.Part;

/**
 * Class that allows for streaming parts of a HTTP Post
 * request.
 * @author Collin
 *
 */
public class MultipartInputStream extends InputStream
{
	/**
	 * The current parts.
	 */
	private Collection<Part> parts;
	
	/**
	 * The input stream of the current part.
	 */
	private InputStream currentPartInput;
	
	/**
	 * The part currently being streamed.
	 */
	private Part currentPart;
	
	/**
	 * The header associated with the part currently
	 * being streamed.
	 */
	private ByteArrayInputStream currentPartHeader;
	
	/**
	 * The multipart footer.
	 */
	private ByteArrayInputStream footer;
	
	/**
	 * An iterator for the parts of this stream.
	 */
	private Iterator<Part> partIt;
	
	/**
	 * The boundary used in this multipart stream.
	 */
	private final String boundary;
	
	/**
	 * Constructor.
	 * @param parts parts used to stream
	 */
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
	
	/**
	 * Gets the generated boundary message of this stream.
	 * @return boundary message
	 */
	public String getBoundary()
	{
		return boundary;
	}
	
	/**
	 * Reads a character from this stream.
	 */
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
	
	/**
	 * Reads from the stream and into the given byte array
	 * at the offset and length.
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		if(b.length == 0)
			return 0;
		
		int totalBytesRead = 0;
		
		while(len > 0 && currentPart != null)
		{
			if(currentPartHeader.available() > 0)
			{
				int bytesRead = currentPartHeader.read(b, off, len);
				len -= bytesRead;
				off += bytesRead;
				totalBytesRead += bytesRead;
			}
			if(len > 0 && currentPartInput.available() > 0)
			{
				int bytesRead = currentPartInput.read(b, off, len);
				len -= bytesRead;
				off += bytesRead;
				totalBytesRead += bytesRead;
			}
			if(len > 0)
			{
				getNextPart();
			}
		}
		
		if(len > 0 && footer.available() > 0)
		{
			totalBytesRead += footer.read(b, off, len);
		}
		
		return totalBytesRead == 0 ? -1 : totalBytesRead;
	}
	
	/**
	 * Closes this stream.
	 */
	@Override
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
	
	/**
	 * Checks how many bytes are available in this stream.
	 * Only checks the bytes in the current part.
	 */
	@Override
	public int available() throws IOException
	{
		var avail = footer.available();
		
		if(currentPart != null)
		{
			avail += currentPartHeader.available() + currentPartInput.available();
		}
		
		return avail;
	}
	
	/**
	 * Cleans up the current part and advances to the next part
	 * if one exists.
	 * @throws IOException
	 */
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
	
	/**
	 * Gets the current part header.
	 * @return part header
	 */
	private byte[] getPartHeader()
	{
		if(currentPart != null)
		{
			return String.format("\r\n--%s\r\nContent-Disposition: %s\r\n\r\n", boundary, currentPart.getHeader("Content-Disposition")).getBytes();
		}

		return new byte[] {};
	}
	
	/**
	 * Gets the current footer
	 * @return footer
	 */
	private byte[] getFooter()
	{
		return String.format("\r\n--%s--", boundary).getBytes();
	}
	
	/**
	 * Closes the current part.
	 * @throws IOException
	 */
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
