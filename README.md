# Game Server Hosting

This repository holds all the source code for the Game Server Hosting project. Since this project was done in the Eclipse IDE, a lot of configuration files from the Eclipse IDE might be fixed with the source code. This makes it easier to import a project into Eclipse.

This project was made to make hosting a game server much more simple. I had a problem where I had to remote into a computer in order to interact with server files, settings, and utimately the server console. I decided to make an application when can give me all that functionality through the browser. From anywhere in my local network, I can access all my servers, have a clean interface to their files and console, and also check the CPU and RAM usage of all the available computers.

Not only did I want to make the managing part as easy as possible, I also wanted to make the deployment part as easy as possible.  To do this, I created two different applications:
1. Controller
2. Node

The Node application is what gets installed into Tomcat on the computer that you want to be physically hosting the server. This application manages getting information in and out of the game server and is also responsible for communicating back its status to the controller and sometimes the browser. I chose the node to communicate to the browser directly in some cases because it would save bandwidth to not have to travel through the controller. Since the application is meant to only be accessed locally, there is not much security risk with direct communication with a Node. If one wanted to change this project to be used across the Internet, then I would highly advise all communications to the browser be through the Controller and the Nodes cannot be accessed directly. One would also need to implement a login system, since this gives access to uploading and deleting of files.

The reason I called the Node application "Node" is because it can be deployed on multiple computers, and allow easy deployment of game servers to any installed computer. This greatly increases productivity of managing servers.

The Controller application gets installed into Tomcat on any computer. This may be the same computer a Node is deployed on or not. This application handles a bulk of the communication to the browser. It is what serves the browser experience. It is the central location for all Node communication. This is possible because in the properties file of the Controller (which is explained in the Installation and Run Guide section), there is the ip addresses of the Nodes.

Current features of this project:

* Easy to use browser experience
* Easy to deploy game servers to any installed computer
* Remotely turn on and off any game server
* File explorer to the the deployed servers
	* Make folders
	* Upload files
	* Download files
	* Rename files
	* Unzip folders into a specific folder
	* Download an entire zip of a specific folder
* Access to the command line interface directly, allowing reading and writing to the program in real-time
* Set up triggers that send commands to the server depending on factors such as time of day, using a timer, or even matching a line of output from the server using regular expressions
* For custom game servers, easily access and change server properties without having to download files, change them, and upload them
* Check the CPU and RAM usage of every machine the Node is running on as well as a nice interface of which game servers are running on which machine and their status in real-time
* Change which file gets executed to run the game server as well as give extra command line arguments for custom game servers

The game servers currently supported:

* Minecraft

Since this project was mainly created for hosting Minecraft servers, some of the general implementation is geared towards Minecraft server functionality/requirements. The server abstraction is not fully complete, but one can still create their own types of game servers by inheriting certain interfaces and creating new tables in their database for their own needs. I have created a library for easily managing database objects, which is part of the DatabaseObjects project in this repo.

# Installation and Run Guide

All installation instructions are located in the "Backend Installation Instructions" document in this repo.

The installation instructions require a .war file of both the GameServerController and GameServerNode Eclipse projects. This can be done by using the Eclipse IDE to export a project into a war file. These .war file will then need to be deployed onto Tomcat through the admin console.

Network factors such as firewalls, port forwarding, and static IPs are not managed by this application and will need to be done by the user manually.