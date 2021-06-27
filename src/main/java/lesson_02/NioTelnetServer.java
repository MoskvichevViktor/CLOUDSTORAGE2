package lesson_02;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NioTelnetServer {
    private static final String LS_COMMAND = "\tls     view all files from current directory";
    private static final String MKDIR_COMMAND = "\tmkdir>.../...  create new dir from current directory";
    private static final String TOUCH_COMMAND = "\ttouch>.../...  create new files from current directory";
    private static final String COPY_COMMAND = "\tcopy>.../...:.../...  copy files from current directory";
    private static final String RM_COMMAND = "\trm>.../...  delete files from current directory";
    private static final String CAT_COMMAND = "\tcat>.../...  text from files";
    private static final String CD_COMMAND = "\tcd..>.../...  getParent";
    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    private Map<SocketAddress, String> clients = new HashMap<>();

    public NioTelnetServer() throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(5679));
        server.configureBlocking(false);
        Selector selector = Selector.open();

        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        while (server.isOpen()) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client connected. IP:" + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "skjghksdhg");
        channel.write(ByteBuffer.wrap("Hello user!\n".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info".getBytes(StandardCharsets.UTF_8)));
    }

    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        int readBytes = channel.read(buffer);

        if (readBytes < 0) {
            channel.close();
            return;
        } else  if (readBytes == 0) {
            return;
        }

        buffer.flip();
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        buffer.clear();

        if (key.isValid()) {
            String command = sb.toString()
                    .replace("\n", "")
                    .replace("\r", "");
            if ("--help".equals(command)) {
                sendMessage(LS_COMMAND, selector, client);
                sendMessage(MKDIR_COMMAND, selector, client);
                sendMessage(TOUCH_COMMAND, selector, client);
                sendMessage(COPY_COMMAND, selector, client);
                sendMessage(RM_COMMAND, selector, client);
                sendMessage(CAT_COMMAND, selector, client);
                sendMessage(CD_COMMAND, selector, client);
            } else if ("ls".equals(command)) {
                sendMessage(getFilesList().concat("\n"), selector, client);
            } else if ("mkdir".equals(command.split(">", 0)[0])) {
                try {
                    String dirname = command.substring(command.lastIndexOf(">") + 1);

                    Path path = Files.createDirectory(Paths.get(dirname));
                } catch(FileAlreadyExistsException e){
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendMessage( "mkdir - successfully", selector, client);
            } else if ("touch".equals(command.split(">", 0)[0])) {

                try {
                    String filename = command.substring(command.lastIndexOf(">") + 1);
                    Path path = Files.createFile(Paths.get(filename));
                } catch (FileAlreadyExistsException e) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendMessage("touch - successfully", selector, client);
            }
            else if ("copy".equals(command.split(">", 0)[0])) {
                try {
                    String s_filename = command.substring(command.lastIndexOf(">") + 1, command.lastIndexOf(":"));
                    String d_filename = command.substring(command.lastIndexOf(":") + 1);
                    System.out.println(d_filename);
                    Path sourcePath      = Paths.get(s_filename);
                    Path destinationPath = Paths.get(d_filename);
                    Files.copy(Paths.get(s_filename), Paths.get(d_filename));
                } catch (FileAlreadyExistsException e) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendMessage("copy - successfully", selector, client);
            }
            else if ("rm".equals(command.split(">", 0)[0])) {


                try {
                    String filename = command.substring(command.lastIndexOf(">") + 1);
                    Path path = Paths.get(filename);
                    Files.delete(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendMessage("rm- successfully", selector, client);
            }

            else if ("cat".equals(command.split(">", 0)[0])) {
                String filename = command.substring(command.lastIndexOf(">") + 1);
                FileChannel c = new RandomAccessFile(filename, "rw").getChannel();
                ByteBuffer buffer = ByteBuffer.allocate(100);
                c.read(buffer);
                buffer.flip();
                byte[] byteBuf = new byte[100];
                int pos = 0;
                while (buffer.hasRemaining()) {
                    byteBuf[pos++] = buffer.get();
                }
                sendMessage((new String(byteBuf, StandardCharsets.UTF_8)), selector, client);
                buffer.rewind();
            }
            else if ("cd..".equals(command.split(">", 0)[0])) {
                String filename = command.substring(command.lastIndexOf(">") + 1);
                Path path = Paths.get(filename);
                Path parent = path.getParent();
                sendMessage(parent.toString(), selector, client);
            }
        }
    }


    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel) key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel) key.channel()).write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    private String getFilesList() {
        String[] server = new File("server").list();
        return String.join(" ", server);
    }

    public static void main(String[] args) throws Exception {
        new NioTelnetServer();
    }
}