# CVE-2017-5638
Apache Struts 2.0 RCE vulnerability

This is a script to exploit CVE-2017-5638 - It allows an attacker to inject OS commands into a web application through the content-type header
Apache Struts 2 is an open-source web application framework for developing Java EE web applications. It uses and extends the Java Servlet API to encourage developers to adopt a model–view–controller (MVC) architecture.

During the last week of March, 2017 an Apache Struts hacker who goes by the name "Nike Zheng" posted a public proof-of-concept exploit showing how easily someone can execute Operating system commands remotely on a Apache Struts 2 vulnerable server.

The vulnerability, CVE-2017-5638 - https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-5638, permits unauthenticated Remote Code Execution (RCE) via a specially crafted Content-Type value in an HTTP request. An attacker can create an invalid value for Content-Type which will cause vulnerable software to throw an exception. When the software is preparing the error message for display, a flaw in the Apache Struts Jakarta Multipart parser causes the malicious Content-Type value to be executed instead of displayed.

Since the release of this public exploit, a large number of scans searching for vulnerable Apache servers were observed. As the exploit is really easy to execute, anyone can exploit vulnerable machines remotely with absolute ease. Thus frequent exploits following the large number of scans were reported.

One of our clients wanted us to scan through all their IPs as they were largely using the vulnerable Apache Struts 2 framework and had no exact records which IP's(Web Servers) were running a vulnerable apache framework. There were close to thousand IP's that were to be tested.
The client demanded to submit all the vulnerable domains along with the information if its a Linux or a Windows Server. In order to avoid the tedious task of scanning each IP individually, I wrote a small script.

The script combines the below functions/tasks:

1. Read the vulnerable domain names one by one from the file containing the domain list.

2. Makes use of the official exploit for checking vulnerable servers.

3. Runs the exploit and check if its a windows or a linux server.
