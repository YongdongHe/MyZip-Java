package com.tencent.mobile.main;


import com.tencent.mobile.main.com.tencent.mobile.zip.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.BitSet;
import java.util.zip.*;

public class Main {

    public static void main(String[] args) {
	// write your code here
        try {

            //ZipFunc();
            //ChanelFunc();
            //RandFunc();
            MyZipFunc();
            //UnZipFunc();
            //DeflaterFunc();
            //System.out.println();


        } catch (IOException e) {
            e.printStackTrace();
        }
        BitBuff n = BitBuff.convert(7);
        BitBuff a = new BitBuff().append(n);
        System.out.println(a);
    }

    public static void ZipFunc()throws IOException{
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream("./test.zip"));
        ZipOutputStream outputStream;
        ZipEntry entry;
        while (null != (entry = zipInputStream.getNextEntry())){

            System.out.println("crc: " + entry.getCrc());
            System.out.println();

        }
    }


    public static void ChanelFunc() throws IOException{

            ByteChannel byteChannel = new FileInputStream("./test.zip").getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(30);
            byteChannel.read(buffer);
            buffer.flip();
            byte[] b = new byte[30];
            System.out.println(buffer.getInt(0));
            IntBuffer buffer1 = IntBuffer.allocate(1);
            System.out.println(0x504b0304L);
            System.out.println(0x04034b50L);
            System.out.println(Arrays.toString(b));
            buffer.rewind();
            while(buffer.hasRemaining()){
                System.out.println(buffer.get());

            }
            byteChannel.read(buffer);

    }

    public static void RandFunc() throws IOException{
        File file = new File("./test.zip");
        MappedByteBuffer out = new RandomAccessFile(file,"r").getChannel().map(FileChannel.MapMode.READ_ONLY,0,100);
        System.out.println(file.length());
        byte[] b = new byte[30];
        System.out.println(out.getInt());
        System.out.println(out.getInt());
        out.position(0);
        System.out.println(out.getInt());
        out.get(b,0,30);
        out.get(b,0,30);
        System.out.println(out.getInt(0));
        System.out.println(0x504b0304L);
        System.out.println(0x04034b50L);
        System.out.println(Arrays.toString(b));



    }

    public static void MyZipFunc()throws IOException{
        MyZipFile file = new MyZipFile("./largepic.zip");
        try {
            file.parseFile();
            for(MyZipEntry entry : file.entryIndices()){
                entry.unpack();
            }
            file.close();
        } catch (ZipFormatException e) {
            e.printStackTrace();
        } catch (UnpackException e1){
            e1.printStackTrace();
        }
//        System.out.println("*******");
//        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream("./yyb.zip"));
//        ZipEntry entry;
//        while (null != (entry = zipInputStream.getNextEntry())){;
//            System.out.println("method : " + entry.getMethod());
//            System.out.println("modiefiedtime : " + entry.getLastModifiedTime());
//            System.out.println("accesstime : " + entry.getLastAccessTime());
//            System.out.println("date : " + "");
//            System.out.println("crc : " + Long.toHexString(entry.getCrc()));
//            System.out.println("compressedSize : " + entry.getCompressedSize());
//            System.out.println("uncompressedSize : " + entry.getSize());
//            System.out.println("fileNameLength : " + entry.getName().length());
//            System.out.println("extraFieldLength : " + Arrays.toString(entry.getExtra()));
//        }
//        zipInputStream.close();
    }

    public static void UnZipFunc()throws IOException {
        long starttime = System.currentTimeMillis();
        String zipFile= "./UnZip.zip";//输入源zip路径
        String outputFolder="C:\\Users\\realhe\\Desktop\\UnZip"; //输出路径（文件夹目录）
        ZipInputStream zis
                = new ZipInputStream(new FileInputStream(zipFile));
        //get the zipped file list entry
        ZipEntry ze = zis.getNextEntry();
        byte[] buffer = new byte[1024];

        while (ze != null) {

            String fileName = ze.getName();
            File newFile = new File(outputFolder + File.separator + fileName);

            System.out.println("file unzip : " + newFile.getAbsoluteFile());
            if (ze.isDirectory()) {
                new File(newFile.getParent()).mkdirs();
            } else {
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
            System.out.println("Done");
        }

        long endTime=System.currentTimeMillis();
        System.out.println("耗费时间： "+(endTime-starttime)+" ms");
    }

    public static void DeflaterFunc()throws IOException{
        Deflater deflater = new Deflater();
        String inputString = "As mentioned above,there are many kinds of wireless systems other than cellular.";
        byte[] input = inputString.getBytes("UTF-8");
        byte[] output = new byte[100];
        Deflater compresser = new Deflater();
        compresser.setInput(input); // 要压缩的数据包
        compresser.finish(); // 完成,
        int compressedDataLength = compresser.deflate(output);
        System.out.println(compressedDataLength);
        System.out.println(Arrays.toString(output));
    }


}
