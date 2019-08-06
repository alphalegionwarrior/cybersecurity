![logo](https://malwared.com/wp-content/uploads/2018/09/byob_black-06.png)
# BYOB (Build Your Own Botnet)
[![license](https://img.shields.io/badge/license-GPL-brightgreen.svg)](https://github.com/colental/byob/blob/master/LICENSE)
[![version](https://img.shields.io/badge/version-0.3-lightgrey.svg)](https://github.com/colental/byob)
[![build](https://img.shields.io/travis/com/malwaredllc/byob/master.svg)](https://travis-ci.com/malwaredllc/byob.svg?branch=master)


__Disclaimer__: This project should be used for authorized testing or educational purposes only.

BYOB is an open-source project that provides a framework for security researchers 
and developers to build and operate a basic botnet to deepen their understanding
of the sophisticated malware that infects millions of devices every year and spawns
modern botnets, in order to improve their ability to develop counter-measures against 
these threats. 

It is designed to allow developers to easily implement their own code and add cool new
features *without* having to write a **RAT** (Remote Administration Tool) or a
**C2** (Command & Control server) from scratch.

*The RAT's key feature is that arbitrary code/files can be remotely loaded into memory
from the C2 and executed on the target machine without writing anything to the disk.*

## Client
[![client](https://img.shields.io/badge/byob-client-blue.svg)](https://github.com/colental/byob/blob/master/byob/client.py)

*Generate fully-undetectable clients with staged payloads, remote imports, and unlimited post-exploitation modules*

1) __Remote Imports__: remotely import third-party packages from the server without writing them 
to the disk or downloading/installing them
2) __Nothing Written To The Disk__: clients never write anything to the disk - not even temporary files (zero IO
system calls are made) because remote imports allow arbitrary code to be 
dynamically loaded into memory and directly imported into the currently running 
process
3) __Zero Dependencies (Not Even Python Itself)__: client runs with just the python standard library, remotely imports any non-standard
packages/modules from the server, and can be compiled with a standalone python 
interpreter into a portable binary executable formatted for any platform/architecture,
allowing it to run on anything, even when Python itself is missing on the target host
4) __Add New Features With Just 1 Click__: any python script, module, or package you to copy to the `./byob/modules/` directory
automatically becomes remotely importable & directly usable by every client while 
your command & control server is running
5) __Write Your Own Modules__: a basic module template is provided in `./byob/modules/` directory to make writing
your own modules a straight-forward, hassle-free process
6) __Run Unlimited Modules Without Bloating File Size__: use remote imports to add unlimited features without adding a single byte to the
client's file size 
7) __Fully Updatable__: each client will periodically check the server for new content available for
remote import, and will dynamically update its in-memory resources
if anything has been added/removed
8) __Platform Independent__: everything is written in Python (a platform-agnostic language) and the clients
generated can optionally be compiled into portable executable (*Windows*) or
bundled into an standalone application (*macOS*)
9) __Bypass Firewalls__: clients connect to the command & control server via reverse TCP connections, which
will bypass most firewalls because the default filter configurations primarily
block incoming connections
10) __Counter-Measure Against Antivirus__: avoids being analyzed by antivirus by blocking processes with names of known antivirus
products from spawning
11) __Encrypt Payloads To Prevent Analysis__: the main client payload is encrypted with a random 256-bit key which exists solely
in the payload stager which is generated along with it
12) __Prevent Reverse-Engineering__: by default, clients will abort execution if a virtual machine or sandbox is detected

## Modules
[![modules](https://img.shields.io/badge/byob-modules-blue.svg)](https://github.com/colental/byob/blob/master/byob/modules)

*Post-exploitation modules that are remotely importable by clients*

1) __Keylogger__ (`byob.modules.keylogger`): logs the user’s keystrokes & the window name entered
2) __Screenshot__ (`byob.modules.screenshot`): take a screenshot of current user’s desktop
3) __Webcam__ (`byob.modules.webcam`): view a live stream or capture image/video from the webcam
4) __Ransom__ (`byob.modules.ransom`): encrypt files & generate random BTC wallet for ransom payment
5) __Outlook__ (`byob.modules.outlook`): read/search/upload emails from the local Outlook client
6) __Packet Sniffer__ (`byob.modules.packetsniffer`): run a packet sniffer on the host network & upload .pcap file
7) __Persistence__ (`byob.modules.persistence`): establish persistence on the host machine using 5 different methods
8) __Phone__ (`byob.modules.phone`): read/search/upload text messages from the client smartphone
9) __Escalate Privileges__ (`byob.modules.escalate`): attempt UAC bypass to gain unauthorized administrator privileges
10) __Port Scanner__ (`byob.modules.portscanner`): scan the local network for other online devices & open ports
11) __Process Control__ (`byob.modules.process`): list/search/kill/monitor currently running processes on the host
12) __iCloud__ (`byob.modules.icloud`): check for logged in iCloud account on macOS

## Server
[![server](https://img.shields.io/badge/byob-server-blue.svg)](https://github.com/colental/byob/blob/master/byob/server.py)

*Command & control server with persistent database and console*

1) __Console-Based User-Interface__: streamlined console interface for controlling client host machines remotely via
reverse TCP shells which provide direct terminal access to the client host machines
2) __Persistent SQLite Database__: lightweight database that stores identifying information about client host machines,
allowing reverse TCP shell sessions to persist through disconnections of arbitrary
duration and enabling long-term reconnaissance
3) __Client-Server Architecture__: all python packages/modules installed locally are automatically made available for clients 
to remotely import without writing them to the disk of the target machines, allowing clients to use modules which require
packages not installed on the target machines

## Core
[![core](https://img.shields.io/badge/byob-core-blue.svg)](https://github.com/colental/byob/blob/master/byob/core)

*Core framework modules used by the generator and the server*

1) __Utilities__ (`byob.core.util`): miscellaneous utility functions that are used by many modules
2) __Security__ (`byob.core.security`): Diffie-Hellman IKE & 3 encryption modes (AES-256-OCB, AES-256-CBC, XOR-128)
3) __Loaders__ (`byob.core.loaders`): remotely import any package/module/scripts from the server
4) __Payloads__ (`byob.core.payloads`): reverse TCP shell designed to remotely import dependencies, packages & modules
5) __Stagers__ (`byob.core.stagers`): generate unique payload stagers to prevent analysis & detection   
6) __Generators__ (`byob.core.generators`): functions which all dynamically generate code for the client generator
7) __Database__ (`byob.core.database`): handles interaction between command & control server and the SQLite database
8) __Handler__ (`byob.core.handler`): HTTP POST request handler for remote file uploads to the server

________________________________________________________________________________________________

### To Do

*Contributors welcome! Feel free to issue pull-requests with any new features or improvements you have come up with!*

1) __Remote Import Encryption__ - encryption for data streams of packages/modules being remotely imported (to maintain confidentiality/authenticity/integrity and prevent any remote code execution vulnerabilities arising from deserialization)
2) __Transport Types__ - add support for more transport types (HTTP/S, DNS, etc.)
3) __Improve Phone/SMS module__ - add spreading ability via emails/text-messages when a contacts list is found on the target host
4) __Improve Outlook/Email module__ - add spreading ability via emails/text-messages when a contacts list is found on the target host
5) __Improve Ransom module__ - add methods for using a Bitcoin exchange API for generating temporary wallet addresses that are unique to the target host, expire after a given number of hours without receiving payment, and which notify the C2 server (maybe via webhooks?) when payment is received or the wallet expires
6) __Support Python 3__ - support both Python 2 and Python 3 in a single codebase
7) __BUG FIXES__ - fix any bugs/issues

________________________________________________________________________________________________

### Contact

__Website__: https://malwared.com

__Email__: security@malwared.com

__Twitter__: [![twitter](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/malwaredllc)
