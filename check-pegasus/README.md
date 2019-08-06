[Article](https://info.lookout.com/rs/051-ESQ-475/images/lookout-pegasus-technical-analysis.pdf) didn't reveal full contents of spyware files so only way to check for presence of these files was to check/compare filenames. But it's unlikely that now after such news coverage they would use same filenames. So instead pgcheck searches for one main Pegasus file which can find without knowing it's name as it's copy of signed binary in iOS (/System/Library/Frameworks/JavaScriptCore.framework/Resources/jsc). Seems 'jsc' has been removed from 9.3.5.

*update v2.8-7* 
- pgcheck is now comparing files to 'jsc' binary which Pegasus copies into `/usr/libexec/` directory. This copy is used to start Pegasus daemons when user restarts device  
- removed deleting of files as modified spyware could use different names 
- log will be written into `/var/mobile/Documents/pgcheck.log` after triggering notification + airplane mode

*update v2.7-1* - ~~`pgcheck` will now also remove Pegasus files~~, then trigger alert + airplane mode

# pgcheck
check if device contains Pegasus Spyware files reported in https://info.lookout.com/rs/051-ESQ-475/images/lookout-pegasus-technical-analysis.pdf

# INSTALLATION
add Cydia repository http://load.sh/cydia/
search for pgcheck

# or 

clone this repository and use `pgcheck` as root

```bash
git clone https://github.com/z448/pgcheck
cd pgcheck/usr/bin
pgcheck i
# respring device
pgcheck
```

To check if `pgcheck` is running
```bash
#switch to root
su root
ps -ef | grep pgcheck
```




# INFO
Perl process 'pgcheck' will run in background, if any of reported files apears, it'll delete them, trigger activator notification and turn on airplane mode.

After rebooting iDevice, LaunchDaemon (/Library/LaunchDaemons/sh.load.pgcheck.plist) will start pgcheck automatically.

# GIF

![pgcheck](https://raw.githubusercontent.com/z448/pgcheck/master/pgcheck.gif)





