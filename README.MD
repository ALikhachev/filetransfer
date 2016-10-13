# File transfer app

Java NIO single-thread file transfer multi-user application TCP-based with message-based protocol for educational purposes.

## How to run
1. Build project (using Maven or how you are used to)
2. Run `ru.nsu.likhachev.network.filetransfer.Server` class.
   The server will listen port defined in `ru.nsu.likhachev.network.filetransfer.Constants` on every available interface
3. Run `ru.nsu.likhachev.network.filetransfer.Client <file path> [server hostname] [server port number]` class.

<> — required, [] — optional

## Strategy
1. To start transfer client sends `CMessageFileMetadata` with file length and name
2. Server responses with `SMessageFileMetadataStatus`
3. Client obtains file id from server's response and starts sending pieces of file
4. Server responses with `SMessageFileDataStatus` for each piece

### There are some shortcomings in terms of performance with this implementation:
* Server uses blocking operations when saving file data

### Some other problems:
* Incomplete documentation
* Very stupid client
* Insufficient input validation  
