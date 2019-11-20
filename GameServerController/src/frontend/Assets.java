package frontend;

import java.util.Random;

public class Assets
{
	public static final String MINECRAFT_BACKGROUNDS_URI = "images/minecraft/";
	private static final Random random = new Random();
	
	public static final String[] MINECRAFT_BACKGROUNDS =
	{
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
	
	public static final String MATERIAL_BACKGROUND = "images/material-back.jpeg";
	public static final String BLACK_BACKGROUND = "images/backdots.jpg";
	
	public static String getRandomMinecraftBackground()
	{
		return MINECRAFT_BACKGROUNDS_URI + MINECRAFT_BACKGROUNDS[random.nextInt(MINECRAFT_BACKGROUNDS.length)];
	}
}
