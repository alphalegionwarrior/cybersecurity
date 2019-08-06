
### Technical summary
As a result of all the manipulations described above, PetrWrap achieves the following goals:
1. The victim’s machine is locked and the MFT of NTFS partitions is encrypted securely (because Petya v3 which is used in this attack doesn’t have flaws of the earlier versions and implements Salsa20 correctly);
2. The lockscreen doesn’t show the flashing skull animation and doesn’t contain any mentions of Petya which makes it harder to assess the situation and determine the extent of the caused damage;
3. The developers of PetrWrap didn’t have to write the low-level bootloader code and risk making mistakes similar to the ones observed in earlier versions of Petya.

### Decryption
Unfortunately, this family of ransomware uses a strong encryption algorithm, meaning a decryption tool is out of the question. However, victims can try restoring files using third-party tools such as R-Studio.

### Detection
Kaspersky products successfully detect this ransomware as Trojan-Ransom.Win32.PetrWrap and PDM:Trojan.Win32.Generic.

### Conclusion
Targeted attacks on organizations with the main aim of encrypting data are becoming more popular. The groups using ransomware in their targeted attacks usually try to find vulnerable servers or servers with unprotected RDP access. After penetrating an organization’s network they use special frameworks like Mimikatz to obtain the necessary credentials for installing ransomware throughout the network. To protect against such attacks, organizations need to keep their server software up to date, use secure passwords for remote access systems, install security solutions on their servers and use security solutions with behavioral detection components on their endpoints.

### virus samples:

Unzip Password:virus
