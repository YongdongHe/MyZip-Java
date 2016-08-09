package com.tencent.mobile.main.com.tencent.mobile.zip;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by realhe on 2016/8/1.
 */
public class UnPackHelper {
    //对应的zip文件
    private File file;
    //解压时需要输出到的目标文件
    private FileOutputStream out;
    //文件的压缩数据段开始的位置
    private long startPosition;
    //文件压缩数据段大小
    private long compressedSize;



    //用于映射文件的压缩数据部分
    private MappedByteBuffer mappedByteBuffer;
    //用于记录文件缓冲区中的数据在文件数据段中的位置
    //TODO  大文件时应该改成long
    private int dataIndex = 0;
    //用于存放从文件中读取的压缩数据段，称为文件缓冲区
    private byte[] dataBuff;
    //文件缓冲区的大小
    private int DATA_BUFF_SIZE = 1024;


    //用于存放当前取出的bit位，对照huffman码表进行解码
    private BitBuff buff;

    //用于存放FileData头
    private BitBuff HEADER = new BitBuff();
    private int HLIT;
    private int HDIST;
    private int HCLEN;

    //动态huffman解码时所需的解码树

    //CL1
    private HashMap<BitBuff,Integer> huffman1;
    //CL2
    private HashMap<BitBuff,Integer> huffman2;
    //CLL
    private HashMap<BitBuff,Integer> huffman3;

    //32KB大小的字典
    public static int deflateDicSize = 32768;
    public static int outputBuffSize = 1;
    private LinkedList<Byte> dictionary;
    private int dic_element_num = 0;
    private ByteBuffer outputBuff;


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


    public void unpack()throws UnpackException,FileNotFoundException{
        //解压前的初始化工作
        String name = "yyb.txt";
        String outPath = "./";
        out = new FileOutputStream(new File(outPath + name));
        dictionary = new LinkedList<>();
        outputBuff = ByteBuffer.allocate(outputBuffSize);

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



    private void dynamicHuffmanUnPack()throws UnpackException{

        HLIT = new BitBuff().append(getBit(5)).reverse().getValue() + 257;
        System.out.println("HLIT" + HLIT);

        HDIST = new BitBuff().append(getBit(5)).reverse().getValue() + 1;
        System.out.println("HDIST" + HDIST);

        HCLEN = new BitBuff().append(getBit(4)).reverse().getValue() + 4;
        System.out.println("HCLEN" + HCLEN);
        initCLL(HCLEN);
        initCL1(HLIT);
        initCL2(HDIST);

        buff.clear();
        try{
            while(true){
                buff.append(getBit());
                if (huffman1.containsKey(buff)){
                    int value = huffman1.get(buff);
                    if (256 == value){
                        Logln("end");
                        break;
                    }else if (value < 256){
                        //说明是literal，直接输出
                        outputByte(value);
                    }else if (value >= 257 && value <= 512){
                        //说明是length
                        int v_length = value - 254;
                        BitBuff dst_buff = new BitBuff();
                        while(!huffman2.containsKey(dst_buff)){
                            //一直取，直到dst_buff可以解码为一个距离
                            dst_buff.append(getBit());
                        }
                        int v_distance = huffman2.get(dst_buff);
                        outputByteWithDistanceAndLength(v_distance,v_length);
                    }else {
                        throw new UnpackException("A value bigger than 256,but not a distance");
                    }
                    buff.clear();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    private void outputByte(int value)throws IOException{
        if (256 ==  value){
            out.write(outputBuff.array());
            outputBuff.clear();
        }
        if (value > 127)
            value = value -256;
        outputBuff.put((byte)value);
        dictionary.offer((byte)value);
        dic_element_num ++;
        if (dic_element_num > deflateDicSize){
            dictionary.peek();
            dic_element_num --;
        }
        if (outputBuff.remaining() == 0){
            out.write(outputBuff.array());
            outputBuff.clear();
        }
    }

    private void outputByteWithDistanceAndLength(int distance , int length)throws UnpackException,IOException{
        if ( distance > dic_element_num )
            throw new UnpackException("Distance beyonds the size of dictionary.");
        int startIndex = dic_element_num - distance;
        ArrayList<Byte> outputValues = new ArrayList<>();
        //此处可能有length超过distance的情况
        for (int i=0;i<length;i++){
            outputValues.add(dictionary.get(startIndex + (i % distance) ));
        }
        for (Byte value : outputValues){
            outputByte((int)value);
        }
    }

    private void initCLL(int cl1_num){
        buff.clear();
        //获得cll序列，总共19项
        int[] clls = new int[19];
        Logln("CLL:");
        for (int i = 0;i<cl1_num;i++){
            buff.clear();
            buff.append(getBit(3)).reverse();
            clls[i] = buff.getValue();
            Log(String.format("%s(%d)",buff.toString(),buff.getValue()));
        }
        int[] currentClls = UnPackUtils.getCurrentClls(clls);
        Logln("\n" + Arrays.toString(currentClls));
        huffman3 = UnPackUtils.getMapOfCCL(currentClls);
        Logln("\nCLL Huffman Hash Map:\n");
        UnPackUtils.printHuffman3Table(huffman3);
    }


    private void initCL1(int cl1_num)throws UnpackException{
        int[] cl1s = getCL(cl1_num);
        Logln("\n" + Arrays.toString(cl1s));
        huffman1 = UnPackUtils.getMapOfCL1(cl1s);
        Logln("\nCL1 Huffman Hash Map:\n");
        UnPackUtils.printHuffman1Table(huffman1);
    }



    private void initCL2(int cl2_num)throws UnpackException{
        int[] cl2s = getCL(cl2_num);
        Logln("\n" + Arrays.toString(cl2s));
        huffman2 = UnPackUtils.getMapOfCL2(cl2s);
        Logln("\nCL2 Huffman Hash Map:\n");
        UnPackUtils.printHuffman2Table(huffman2);
    }


    private int[] getCL(int cl_num)throws UnpackException{
        BitBuff flag = new BitBuff();
        buff.clear();
        int cl_count = 0;
        int[] cls = new int[cl_num];
        try{
            while(cl_count < cl_num){
                buff.append(getBit(1));
                if (huffman3.containsKey(buff)){
                    //已在huffman叶子节点命中
                    int cl1_value = huffman3.get(buff);
                    if (16 == cl1_value){
                        //如果是16的话，后两位记载了cl1_value的重复次数
                        flag.clear();
                        flag.append(getBit(2)).reverse();
                        int repeat_value = cls[cl_count-1];
                        int repeat_times = 3 + flag.getValue();
                        for (int i = cl_count;i < cl_count + repeat_times ; i++){
                            cls[i] = repeat_value;
                        }
                        cl_count += repeat_times;
                    }else if (17 == cl1_value){
                        flag.clear();
                        flag.append(getBit(3)).reverse();
                        int repeat_value = 0;
                        int repeat_times = 3 + flag.getValue();
                        for (int i = cl_count; i <cl_count + repeat_times; i++){
                            cls[i] = repeat_value;
                        }
                        cl_count += repeat_times;
                    }else if (18 == cl1_value){
                        flag.clear();
                        flag.append(getBit(7)).reverse();
                        int repeat_value = 0;
                        int repeat_times = 11 + flag.getValue();
                        for (int i = cl_count; i <cl_count + repeat_times; i++){
                            cls[i] = repeat_value;
                        }
                        cl_count += repeat_times;
                    }else {
                        cls[cl_count] = cl1_value;
                        cl_count += 1;
                    }
                    buff.clear();
                }
            }
            return cls;
        }catch (IndexOutOfBoundsException e){
            e.printStackTrace();
            throw new UnpackException("Wrong cl1 arrays");
        }
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
                dataIndex += DATA_BUFF_SIZE;
            }
            else{
                mappedByteBuffer.get(dataBuff,dataIndex,mappedByteBuffer.remaining());
                dataIndex += mappedByteBuffer.remaining();
            }

        }
        byte next_byte = dataBuff[byteIndex];
        byteIndex = (byteIndex + 1)%DATA_BUFF_SIZE;
        return next_byte;
    }

}
