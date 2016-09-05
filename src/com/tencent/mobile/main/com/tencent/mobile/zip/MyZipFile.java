package com.tencent.mobile.main.com.tencent.mobile.zip;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Created by realhe on 2016/7/21.
 */
public class MyZipFile implements MyZipConstants {
    public final static int defaultBufferSize = 216;
    //Zip文件
    File file;
    //用来读取文件的缓冲区
    MappedByteBuffer mappedByteBuffer;
    //用来读取缓冲区内容，按指定格式填充变量的方法
    MyZipUtils myZipUtils;
    //隧道
    FileChannel channel;
    //文件数据块索引
    ArrayList<MyZipEntry> myZipEntries;
    public MyZipFile(String fileName)throws IOException{
        file = new File(fileName);
        init();

    }

    public MyZipFile(File file)throws IOException{
        this.file = file;
        init();
    }

    private void init() throws IOException{
        myZipEntries = new ArrayList<>();
        channel = new RandomAccessFile(file,"rw")
                .getChannel();
    }

    public void unpack(String path)throws IOException,UnpackException{
        if (!path.endsWith("/"))
            throw new UnpackException("The unpack path is not a directory.");
        for(MyZipEntry entry : this.entryIndices()){
            entry.unpack(path);
            System.out.println(entry.getFileName());
        }
    }

    public void parseFile()throws IOException,ZipFormatException{
        parseLocalFile();
    }


    private void parseLocalFile()throws IOException,ZipFormatException{
        long position = 0;
        //TODO  判断文件是否为最后一个压缩数据段
        while (position<file.length()){
            if (file.length() - position < 30){
                //剩余部分已无压缩文件
                return;
            }
            mappedByteBuffer =  channel.map(FileChannel.MapMode.READ_ONLY,position,file.length()-position);
            byte[] locBytes = new byte[30];
            mappedByteBuffer.get(locBytes);
            if (MyZipUtils.get32(locBytes,0) != LOCSIG){
                //头校验，如果不相等说明格式错误或者LOC部分已经结束了
                return;
            }
            MyZipEntry entryIndex = new MyZipEntry(locBytes,file);
            byte[] nameBytes = new byte[entryIndex.getFileNameLength()];
            byte[] extrafieldBytes = new byte[entryIndex.getExtraFieldLength()];
            mappedByteBuffer.get(nameBytes);
            mappedByteBuffer.get(extrafieldBytes);
            entryIndex.setFilename(new String(nameBytes, "gbk"));
            entryIndex.setExtraField(new String(extrafieldBytes));
            entryIndex.setStartPosition(position);
            myZipEntries.add(entryIndex);
            //移动光标到文件末尾
            position += entryIndex.getLength();
        }
    }


    private void parseFileDirectory()throws IOException,ZipFormatException{
        
    }

    private long getFileDirectoryPosition()throws IOException,ZipFormatException{
        long endSize = 0;
        long fileLen = file.length();
        if(file.length() <= MyZipConstants.MaxEndSize  ){
            mappedByteBuffer =  channel.map(FileChannel.MapMode.READ_ONLY,0,fileLen);
            endSize = fileLen;
        }else {
            mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY,fileLen - MyZipConstants.MaxEndSize ,MyZipConstants.MaxEndSize);
            endSize = MyZipConstants.MaxEndSize;
        }
        mappedByteBuffer.flip();
        //// TODO: 2016/8/27 完成CD的解码
        return 1;
    }

    private long getLength(){
        return file.length();
    }

    public ArrayList<MyZipEntry> entryIndices(){
        return myZipEntries;
    }

    public void close() throws IOException {
        channel.close();
    }



}
