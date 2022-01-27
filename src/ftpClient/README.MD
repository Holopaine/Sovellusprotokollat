## Simple FTP client
Logs into Funet FTP server and allows the user to use LIST and RETR commands for
downloading images of old maps. Files are downloaded in working directory. 

### To run:
`java FtpCleint.java`

```
Available commands:
LIST [dir]: List directory content.
RETR <filepath>: Download a file from the server to the working directory
QUIT: End session.
```