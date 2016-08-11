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

    //静态huffman编码
    private static HashMap<BitBuff,Integer> huffman_dist;
    private static HashMap<BitBuff,Integer> huffman_lit;

    //32KB大小的字典
    public static final int deflateDicSize = 32768;
    public static final int outputBuffSize = 1;
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
    }


    public void unpack() throws UnpackException, IOException {
        //解压前的初始化工作
        String name = "pic.jpg";
        String outPath = "./";
        out = new FileOutputStream(new File(outPath + name));
        dictionary = new LinkedList<>();
        outputBuff = ByteBuffer.allocate(outputBuffSize);

        buff.clear();
        boolean isEnd = false;
        while (!isEnd){
            HEADER.clear();
            HEADER.append(getBit(3));
            if(HEADER.get(0)){
                //HEADER第一位是1时，说明是最后一个数据段,本次解码后便结束解码
                isEnd = true;
            }
            if ( HEADER.get(2) && !HEADER.get(1)){
                //HEADER = 10 动态huffman
                dynamicHuffmanUnPack();
            }else if (!HEADER.get(2) && HEADER.get(1)){
                //HEADER = 01 静态huffman
                System.out.println("FixedCode");
                fixedHuffmanUnPack();
                System.out.println("FixedCode End");
            }else if (!HEADER.get(2) && !HEADER.get(1)){
                //HEADER = 00 直接存储
                System.out.println("Stored");
                nocompressedUnPack();
            }else {
                throw new UnpackException("reserved compressed data header(error)");
            }
        }
        out.close();
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
                        if (v_distance == 1707){
                            int a = 0;
                        }
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

    private void fixedHuffmanUnPack()throws UnpackException{
        if (huffman_dist == null || huffman_lit == null){
            huffman_dist = UnPackUtils.getFixedDistTable();
            huffman_lit =  UnPackUtils.getFixedLitTable();
            UnPackUtils.printHuffman1Table(huffman_lit);
            UnPackUtils.printHuffman2Table(huffman_dist);
        }
        buff.clear();
        try{
            while(true){
                buff.append(getBit());
                if (huffman_lit.containsKey(buff)){
                    int value = huffman_lit.get(buff);
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
                        while(!huffman_dist.containsKey(dst_buff)){
                            //一直取，直到dst_buff可以解码为一个距离
                            dst_buff.append(getBit());
                        }
                        int v_distance = huffman_dist.get(dst_buff);
                        if (v_distance == 1707){
                            int a = 0;
                        }
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

    private void nocompressedUnPack()throws UnpackException{
        //skip to next word
        skipByte();
        //get the len
        long LEN = 0x00;
        byte byte0 = getByte();
        byte byte1 = getByte();
        byte byte2 = getByte();
        byte byte3 = getByte();
        LEN |= byte1;
        LEN |= (byte0<<8);
        LEN |= (byte2<<24);
        LEN |= (byte3<<16);
        int l = 0;
        try{
            for (long num = 0; num < LEN ; num ++){
                outputStoredByte(getByte());
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
//        System.out.print((char)value);
        dictionary.offer((byte)value);
        dic_element_num += 1;
        while (dic_element_num > deflateDicSize){
            dictionary.poll();
            dic_element_num -= 1;
        }
        if (outputBuff.remaining() == 0){
            out.write(outputBuff.array());
            outputBuff.clear();
        }
    }

    private void outputStoredByte(byte value)throws IOException{
        outputBuff.put(value);
        dictionary.offer(value);
        dic_element_num += 1;
        while (dic_element_num > deflateDicSize){
            dictionary.poll();
            dic_element_num -= 1;
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
            throw new UnpackException("Wrong cl arrays");
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

    private void skipByte(){
        //跳过当前字节后面的位数，下一次直接从下个字节开始取
        bitIndex = 0;
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
    long totalIndex = 0;
    //从文件缓冲区中获得一个字节
    private byte getByte(){
        //TODO  对compressedSize超出int表示范围时仍需要修改
        long remaining;
        if (byteIndex == 0){
            remaining = mappedByteBuffer.remaining();
            if (remaining > DATA_BUFF_SIZE){
                dataBuff = new byte[DATA_BUFF_SIZE];
                totalIndex += DATA_BUFF_SIZE;
            }
            else{
                //remaining已经小于DATA_BUFF_SIZE了
                dataBuff = new byte[(int)remaining];
                totalIndex += remaining;
            }
            try{
                mappedByteBuffer.get(dataBuff,0,dataBuff.length);
            }catch (IndexOutOfBoundsException e){
                e.printStackTrace();
            }

        }
        try{
            byte next_byte = dataBuff[byteIndex];
            byteIndex = (byteIndex + 1)%dataBuff.length;
            return next_byte;
        }catch (IndexOutOfBoundsException e){
            e.printStackTrace();
            try {
                out.write(outputBuff.array());
                out.flush();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return 0x00;
    }

}
