# Game Server Hosting

This repository holds all the source code for the Game Server Hosting project.

This project was made to make hosting a game server much more simple. I had a problem where I had to remote into a computer in order to interact with server files, settings, and utimately the server console. I decided to make an application when can give me all that functionality through the browser. From anywhere in my local network, I can access all my servers, have a clean interface to their files and console, and also check the CPU and RAM usage of all the available computers.

Not only did I want to make the managing part as easy as possible, I also wanted to make the deployment part as easy as possible.  To do this, I created two different applications:
1. Controller
2. Node

The Node application is what gets installed into Tomcat on the computer that you want to be physically hosting the server. This application manages getting information in and out of the game server and is also responsible for communicating back its status to the controller and sometimes the browser. I chose the node to communicate to the browser directly in some cases because it would save bandwidth to not have to travel through the controller. Since the application is meant to only be accessed locally, there is not much security risk with direct communication with a Node. If one wanted to change this project to be used across the Internet, then I would highly advise all communications to the browser be through the Controller and the Nodes cannot be accessed directly. One would also need to implement a login system, since this gives access to uploading and deleting of files.

The reason I called the Node application "Node" is because it can be deployed on multiple computers, and allow easy deployment of game servers to any installed computer. This greatly increases productivity of managing servers.

The Controller application gets installed into Tomcat on any computer. This may be the same computer a Node is deployed on or not. This application handles a bulk of the communication to the browser. It is what serves the browser experience. It is the central location for all Node communication.

Current features of this project:

* Easy to use browser experience
* Easy to deploy game servers to any installed computer
* Remotely turn on and off any game server
* File explorer to the deployed servers
	* Make folders
	* Upload files
	* Download files
	* Rename files
	* Unzip folders into a specific folder
	* Download an entire zip of a specific folder
* Access to the command line interface directly, allowing reading and writing to the program in real-time
* Set up triggers that send commands to the server depending on factors such as time of day, using a timer, or even matching a line of server output using regular expressions
* For custom game servers, easily access and change server properties without having to download files, change them, and upload them
* Check the CPU and RAM usage of every machine the Node is running on as well as a nice interface of which game servers are running on which machine and their status in real-time
* Change which file gets executed to run the game server as well as give extra command line arguments for custom game servers

The game servers currently supported:

* Minecraft
* ... Practically anything else!

As of Version 2, you may now write code to host any kind of program! (Not at all tested with anything other than the minecraft server code). There are a few things to keep in mind when developing for this platform.

I really wanted to keep using the facilities that I have been using prior to version 2. This means being able to still use the libraries in the custom code, as well as hook into the main application for normal things. Well you can! Here's how it works:

You write code that adheres to the `GameServerModule.java` interface. As long as your module can implement the various methods, it should just work. How do we get this code onto the platform? Well through the UI there is a page for uploading a Jar file. Upload the Jar containing all the code necessary for your plugin to work. The platform will then search for your implementation and use it. This implementation is then saved to the database to be retrieved whenever you boot up the application. Make changes? No worries, just use the UI to update the Jar file and the various node applications will promptly switch their implementations to your new one. If you had servers currently running, then those servers are shut off, updated, and then automatically switched back on. However, your server type must be unique across all plugins.

What you can customize:
* GameServerOptions: An class that contains various general options/settings about your module
* GameServerCommandHandler: Allows you to plugin to the existing command handling framework so that you can directly talk to servers running on nodes.
* GameServerUIHandler: Create custom pages for your application, making what you can do limitless! (Ability to customize the existing pages are being researched).

But, wait... My application requires files such as css, html, and javascript. How can I use those? That's where the `resources` folder comes in. When you package your Jar up, any file resources that you may want to use must be placed in a folder called `resources` (You may make any folders/files within this). Whenever you upload/update your code to the platform, it will search for the `resources` folder and place that into the the folder where static files are held. Won't this cause conflicts? Well this is how it works:

This is your resources directory:
* resources/images/hello.png
* resources/images/background.jpg
* resources/stuff.txt
* resources/configuration/settings/application.ini

If your server type is 'MyOwnGame', then your items will be placed in the static folder as such:
* images/MyOwnGame/hello.png
* images/MyOwnGame/background.jpg
* MyOwnGame/stuff.txt
* configuration/settings/MyOwnGame/application.ini

This is to prevent your files from squashing any of my files for the platform while allowing the Tomcat server to easily access them. These files are updated every time you upload a new Jar.

Current dependencies:
* Tomcat 8/9 (http://tomcat.apache.org/): This is used both as a platform to run the applications and as a library dependency for http/web socket programming.
* Java-ORM (https://github.com/mooncollin/Java-ORM): These requires jarring this project up and putting it into Tomcat's lib directory.
* Apache Velocity (http://velocity.apache.org/): I use this to generate the HTML pages. This and its dependencies need to be placed in the lib directory of the GameServerController.
* MYSQL Connector: I have included the jar that I currently use in this repo. This is just easier to manage. This is used in both the controller and node applications to communicate with the database. Place this inside the lib folder of the Tomcat installation directory.
* JSON Simple (https://code.google.com/archive/p/json-simple/): A library for using and parsing the JSON message format. This is to be placed in the lib directory of the GameServerController as well as the GameServerNode.
* GameServerNode: So this is more a dependency for the Controller. The controller uses code from the GameServerNode section for various things. For this reason, it must be placed in the lib folder of the Controller application and NOT in the lib folder of Tomcat. If placed in the lib folder of Tomcat, it will break.

# Installation and Run Guide

All installation instructions are located in the "Backend Installation Instructions" document in this repo.

The installation instructions require a .war file of both the GameServerController and GameServerNode Eclipse projects. This can be done by using the Eclipse IDE to export a project into a war file. These war files will then need to be deployed onto Tomcat through the admin console.

Network factors such as firewalls, port forwarding, and static IPs are not managed by this application and will need to be done by the user manually.

# Changelog

## Version 2.1
* Fixed [issue #1](https://github.com/mooncollin/Game-Server-Hosting/issues/1)
* Fixed [issue #2](https://github.com/mooncollin/Game-Server-Hosting/issues/2)
* Fixed [issue #3](https://github.com/mooncollin/Game-Server-Hosting/issues/3)
* Fixed [issue #5](https://github.com/mooncollin/Game-Server-Hosting/issues/5)
* Fixed [issue #6](https://github.com/mooncollin/Game-Server-Hosting/issues/6)
* Node backend now uses Endpoints framework, just like the GameServerController

## Version 2.0!!
Custom game server update!

Custom games can now be created to run on the platform!

### Changes to the original Minecraft game:
The original Minecraft game that was built in is now a standalone implementation of the GameServerModule interface. It will exist in the repo (alongside any other servers I feel like implementing) so that others may use them.

Nothing major should have changed and all previous functionality should be there.

### Front-end Features:
* Added a new Game Types page to manage your different game types

## Version 1.5
UI Update, trigger fix, and other fixes

### Front-end Fixes:
* Switched back to custom console interface. Lots of problems with the other one. Hopefully this time it works properly (Mobile has to refresh to see console feed).
* Stop server buttons work on server page and nodes page once again.

### Front-end Features:
* Changed UI a bit again. Looks A LOT nicer on smaller screens.
* Add server is now on the left side near servers in navigation
* Removed Add Game Type button as that hasn't been implemented yet
* Added Follow button to console to trail the console

### Back-end Fixes:
* Triggers work again

## Version 1.4
UI Update and bug fixes

### Front-end Fixes:
* Can now edit settings correctly on Linux
* Refactored node command/socket connections

### Front-end Features:
* Nicer look to navigation
* Added Restart button to console page
* Added Copy Address button to console page (Gets the public IP address and port of the server)
* Using a new console interface! (https://terminal.jcubic.pl/)

	Now warning messages will appear in yellow, error messages in red, and hyperlinks in blue.

### Back-end Features:
* Redesigned URL endpoint creation (again) to be used easier
* Added a restart method to game servers
* Added a public ip address method to game servers

## Version 1.3
Major bug fix update!

### Front-end Fixes:
* Auto-restart is now set to off by default
* Adding a server now does not just copy properties from another existing server
* Can change server properties correctly
* Can delete files correctly

### Front-end Changes:
* Now using Apache Velocity to generate HTML instead of HTMLRenderingTool

### Back-end Fixes:
* Using standard way of authenticating request data

### Back-end Changes:
* Output from server now dealt differently (hopefully a bit more efficient)

## Version 1.2.2
Fixed a major bug that disallowed server starting

## Version 1.2.1
Bug Fixes and URL Endpoint changes

### Back-end Features:
* Fixed a bug related to servers with spaces in their names.
* Changed how URL endpoint are generated for each api endpoint. All parameter variables are now encoded correctly.

## Version 1.2
Lots more bug fixes and more features!

### Front-end Features:
* Triggers now work again!
* Recurring triggers now uses the format HH:MM:SS in the text field
* Output triggers can now use regex groups in the command! Just use $1 for the first group, $2 for the second, and so on (Note: To use regex, wrap your value in "r/YOUR MESSAGE HERE/").
* When starting and stopping a server, a spinner will appear next to the button to indicate that it is processing. This will only be noticable on the same page. Other users shutting down/starting up a server will not show this indicator on your browser.
* Simplified front-end websocket connections and javascript logic

### Back-end Features:
* Changed how those accepting output from a server gets notified when they have been written to.
* Notifiers to server output and to server on/off state changes are now possibly paralleled.
* Output coming from the MinecraftServer object now sends to its output connectors in parallel.
* Writing to the web socket connection a servers' output is now vastly simplified, more generic, and much faster.
* Various bug fixes and improvements.

## Version 1.1
Various back-end improvements and bug fixes.

### Minor front-end features:
* Cannot make a server name with symbols, except for underscore (_)
* Fixed a bug where node resources were not showing on the website when no servers are added

### Major back-end features:
* Database will clear table entries associated with a game server when that game server gets deleted
* Changed database to use integer IDs that are auto-generated instead of the server names
* The application will now auto-generate the gameserver, minecraftserver, node, and triggers tables if they are not already created. Both the controller and node will attempt to do this if either one is started first (recommend doing this since it will do it correctly everytime).
* Enhanced database ORM library to support joins
* Updated htmlrenderingtool library to no longer use reflection and massively simplified implementing from html attributes
* Html rendering is now in its own separate package along with using a different style in creating web pages
* Uploads and downloads of files and folders are now pushing its data directly to the requesting server/computer. It is no longer buffering and sending all at once (Transfers are much faster now).
* More logging as been put in place in situations where the application has no other option but to send a 500 status error
* All api endpoints (Controller and Node) now have a way to generate those endpoints given their query parameters