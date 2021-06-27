package lesson_02;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class BufferInfo {
    public static void main(String[] args) throws IOException {


        FileChannel channel = new RandomAccessFile("client" + File.separator + "1.txt", "rw").getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(10);

        channel.read(buffer);
        buffer.flip(); // limit = pos, pos = 0

        System.out.println(buffer);

        byte[] byteBuf = new byte[10];
        int pos = 0;
        while (buffer.hasRemaining()) {
            byteBuf[pos++] = buffer.get();
        }
        System.out.println(new String(byteBuf, StandardCharsets.UTF_8));

        buffer.rewind();
    }
}
