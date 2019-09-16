package frontend;

import java.util.Random;

public class Assets
{
	public static final String MINECRAFT_BACKGROUNDS_URI = "images/minecraft/";
	
	public static final String[] MINECRAFT_BACKGROUNDS = {
			"background1.jpg",
			"background2.jpg",
			"background3.jpg",
			"background4.jpg",
			"background5.jpg",
			"background6.jpg",
			"background7.jpg",
			"background8.jpg",
			"background9.jpg"
	};
	
	public static String getRandomMinecraftBackground()
	{
		Random r = new Random();
		
		return MINECRAFT_BACKGROUNDS_URI + MINECRAFT_BACKGROUNDS[r.nextInt(MINECRAFT_BACKGROUNDS.length)];
	}
}
