# CS3103

## Team Members
- Cao Liang
- Justin Poh
- Sky Ang
- Jason Leo

### Assumption made for our protocol designs (Subject to changes)
- Assuming user will only download/upload text files
- Assuming the chunk size per file is at least 10
- Set chunk size to be 1,200 bytes
- Assuming user is willing to advertise/upload a particular file when he/she downloaded the entire file from his/her peers\
- Assuming peer download to/upload from one single folder during each session
- Assuming peers will not add new files to the folder after the initial advertisments
- Advertising of files will be automatically peformed by the program when: 
  1) The client start the session(start the program for the first time) and the program will advertise all files in the folder provided by    user. 
  2) Whenever client downloaded an entire file from a peer, the program will automatically advertised the downloaded file.
