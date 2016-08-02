package com.tencent.mobile.main.com.tencent.mobile.zip;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by realhe on 2016/8/1.
 */
public class UnPackHelper {
    File file;
    //文件的压缩数据段开始的位置
    long startPosition;
    //文件压缩数据段大小
    long compressedSize;

    //用于映射文件的压缩数据部分
    MappedByteBuffer mappedByteBuffer;
    //用于记录文件缓冲区中的数据在文件数据段中的位置
    //TODO  大文件时应该改成long
    int dataIndex = 0;
    //用于存放从文件中读取的压缩数据段，称为文件缓冲区
    byte[] dataBuff;
    //文件缓冲区的大小
    int DATA_BUFF_SIZE = 1024;


    //用于存放当前取出的bit位，对照huffman码表进行解码
    BitBuff buff;

    //用于存放FileData头
    BitBuff HEADER = new BitBuff();
    int HLIT;
    int HDIST;
    int HCLEN;

    //动态huffman解码时所需的解码树
    HashMap<BitBuff,Integer> huffman1;
    HashMap<BitBuff,Integer> huffman2;
    HashMap<BitBuff,Integer> huffman3;


    public UnPackHelper(File file, long startPosition, long compressedSize) throws IOException {
        this.file = file;
        this.startPosition = startPosition;
        this.compressedSize = compressedSize;
        this.mappedByteBuffer =  new RandomAccessFile(file,"rw")
                .getChannel().map(FileChannel.MapMode.READ_ONLY,startPosition,compressedSize);
        dataBuff = new byte[DATA_BUFF_SIZE];
        buff = new BitBuff();
        dataIndex = 0;
    }


    public void unpack(){
        buff.clear();
        HEADER.append(getBit(3));
        if ( HEADER.get(2) && !HEADER.get(1)){
            //HEADER = 10 动态huffman
            dynamicHuffmanUnPack();
        }else if (!HEADER.get(2) || HEADER.get(1)){
            //HEADER = 01 静态huffman
        }else if (!HEADER.get(2) || !HEADER.get(1)){
            //HEADER = 00 直接存储
        }
    }

    private void dynamicHuffmanUnPack(){
        System.out.println("^");

        HLIT = new BitBuff().append(getBit(5)).reverse().getValue() + 257;
        System.out.println("HLIT" + HLIT);

        HDIST = new BitBuff().append(getBit(5)).reverse().getValue() + 1;
        System.out.println("HDIST" + HDIST);

        HCLEN = new BitBuff().append(getBit(4)).reverse().getValue() + 4;
        System.out.println("HCLEN" + HCLEN);
        getCLL(HCLEN);
    }

    private void getCLL(int cll_length){
        buff.clear();
        //获得cll序列，总共
        int[] clls = new int[19];
        Logln("CLL:");
        for (int i = 0;i<cll_length;i++){
            buff.clear();
            buff.append(getBit(3)).reverse();
            clls[i] = buff.getValue();
            Log(String.format("%s(%d)",buff.toString(),buff.getValue()));
        }
        int[] currentClls = UnPackUtils.getCurrentClls(clls);
        Logln(Arrays.toString(currentClls));
    }

    private void Log(String logMsg){
        System.out.print(logMsg);
    }

    private void Logln(String logMsg){
        System.out.println(logMsg);
    }





    //用从字节缓冲区内按低位有限取出，将从0到7循环变化
    int bitIndex = 0;
    //用于存放最后取出的字节
    BitBuff byteBuff;


    //从字节缓冲区内获得一个bit
    private boolean getBit(){
        if (bitIndex == 0){
            byte next_byte = getByte();
            byteBuff = UnPackUtils.getBitBuff(next_byte,8);
            //System.out.println(byteBuff.toString());
        }

        boolean bit = byteBuff.get(bitIndex);
        bitIndex = (bitIndex + 1)%8;
        return bit;
    }

    //从字节缓冲区内获得num个bit
    private boolean[] getBit(int num){
        boolean[] bits = new boolean[num];
        for (int i = 0;i<num;i++){
            bits[i] = getBit();
        }
        return bits;
    }

    //记录下一个从文件缓冲区中被取出的字节的位置
    int byteIndex = 0;

    //从文件缓冲区中获得一个字节
    private byte getByte(){
        if (byteIndex == 0){
            if (mappedByteBuffer.remaining() > DATA_BUFF_SIZE){
                mappedByteBuffer.get(dataBuff,dataIndex,DATA_BUFF_SIZE);
            }
            else{
                mappedByteBuffer.get(dataBuff,dataIndex,mappedByteBuffer.remaining());
            }
            dataIndex += DATA_BUFF_SIZE;
        }
        byte next_byte = dataBuff[byteIndex];
        byteIndex = (byteIndex + 1)%DATA_BUFF_SIZE;
        return next_byte;
    }

}
