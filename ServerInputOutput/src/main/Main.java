package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Scanner;

public class Main
{
	public static final String serverLocation = "E:\\Minecraft Server Deployment\\backend\\ServerInputOutput\\src\\resources\\server.jar";
	public static final File serverFolderLocation = new File("E:\\\\Minecraft Server Deployment\\\\backend\\\\ServerInputOutput\\\\src\\\\resources");
	
	public static final String[] serverRunCommands = {
			"java",
			"-Xmx1024M",
			"-Xms1024M",
			"-jar",
			serverLocation,
			"nogui"
	};
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		Runtime rt = Runtime.getRuntime();
		Process p = rt.exec(serverRunCommands, null, serverFolderLocation);
		Thread t = new Thread() {
			public void run()
			{
				InputStream stream = p.getInputStream();
				while(p.isAlive())
				{
					try
					{
						while(stream.available() > 0)
						{
							if(interrupted())
							{
								break;
							}
							System.out.print((char) stream.read());
						}
						Thread.sleep(50);
					} catch (IOException | InterruptedException e)
					{
						return;
					}
				}
			}
		};
		Scanner in = new Scanner(System.in);
		System.out.println("Running server with PID: " + p.pid());
		t.start();
		
		OutputStream outToServer = p.getOutputStream();
		BufferedReader inToServer = new BufferedReader(new InputStreamReader(System.in));
		while(p.isAlive())
		{
			while(inToServer.ready())
			{
				System.out.println("We are writing!");
				outToServer.write(inToServer.readLine().getBytes());
				outToServer.write('\n');
				outToServer.flush();
			}
			Thread.sleep(50);
		}
		
		in.close();
	}
}
