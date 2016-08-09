package com.tencent.mobile.main.com.tencent.mobile.zip;

import java.util.BitSet;
import java.util.HashMap;

/**
 * Created by realhe on 2016/8/1.
 */
public class UnPackUtils {
    public static int[][] DISTANCE_GROUP_CODE =
    {
        {1},
        {2},
        {3},
        {4},
        range(5,7),
        range(7,9),
        range(9,13),
        range(13,17),
        range(17,25),
        range(25,33),
        range(33,49),
        range(49,65),
        range(65,97),
        range(97,129),
        range(129,193),
        range(193,257),
        range(257,385),
        range(385,513),
        range(513,769),
        range(769,1025),
        range(1025,1537),
        range(1537,2049),
        range(2049,3073),
        range(3073,4097),
        range(4097,6145),
        range(6145,8193),
        range(8193,12289),
        range(12289,16385),
        range(16385,24577),
        range(24577,32769)
    };

    public static int[][] LENGTH_GROUP_CODE = {
        {3},
        {4},
        {5},
        {6},
        {7},
        {8},
        {9},
        {10},
        range(11,13),
        range(13,15),
        range(15,17),
        range(17,19),
        range(19,23),
        range(23,27),
        range(27,31),
        range(31,35),
        range(35,43),
        range(43,51),
        range(51,59),
        range(59,67),
        range(67,83),
        range(83,99),
        range(99,115),
        range(115,131),
        range(131,163),
        range(163,195),
        range(195,227),
        range(227,258),
        {258}
    };



    public static int[] range(int startIndex,int endIndex){
        int[] rangeArray = new int[endIndex - startIndex];
        for (int i = 0 ; i<endIndex - startIndex ; i++ ){
            rangeArray[i] = i + startIndex;
        }
        return rangeArray;
    }


    //例如byte为0xfe，那么得到的bitBuff就是01111111
    public static BitBuff getBitBuff(byte byteTemp,int length){
        BitBuff bs = new BitBuff(length);
        bs.clear();
        byte or = 0x01;
        for (int i = 0;i < length ;i++){
            if (0x00 == (byteTemp&or)) {
                bs.set(i,false);
            }
            else {
                bs.set(i,true);
            }
            or <<= 1;
        }
        return bs;
    }

    public static int[] current_cll_index = new int[]{
         3,17,15,13,11,9,7,5,4,6,8,10,12,14,16,18,0,1,2
    };

    //获得置换后的cll
    public static int[] getCurrentClls(int[] clls){
        int[] curren_clls = new int[19];
        for (int i = 0;i<19;i++){
            curren_clls[i] = clls[current_cll_index[i]];
        }
        return curren_clls;
    }

    public static HashMap<BitBuff,Integer> getMapOfCCL(int[] clls){
        //ccls中出现的最大值（huffman的高度）
        int MAX_BITS = getMaxValue(clls);
        //对huffman树每层叶子节点进行统计
        int[] bl_count = new int[MAX_BITS + 1];
        for (int i = 0;i<clls.length;i++){
            bl_count[clls[i]] ++;
        }
        //当前层最左边的叶子节点的码字的值
        int code = 0;
        //将对0的统计清空
        bl_count[0] = 0;
        //存储每一层最左边的叶子节点的码字
        int[] next_code = new int[MAX_BITS + 1];

        for (int i = 1; i < MAX_BITS + 1;i++){
            code = (code + bl_count[i-1]) << 1;
            next_code[i] = code;
        }

        HashMap<BitBuff,Integer> map_cll = new HashMap<>();

        for (int i = 0; i < clls.length ; i++ ){
            //n为第i项cll所表示的码字长度
            int n_clen = clls[i];
            if (n_clen!=0){
                BitBuff huffman_code = getInfatingBinary(BitBuff.convert(next_code[n_clen]),n_clen);
                map_cll.put(huffman_code,i);
                next_code[n_clen] ++;
            }
        }
        return map_cll;
    }

    public static HashMap<BitBuff,Integer> getMapOfCL1(int[] cl1s){
        //cl1s中出现的最大值（huffman的高度）
        int MAX_BITS = getMaxValue(cl1s);
        //对huffman树每层叶子节点进行统计
        int[] bl_count = new int[MAX_BITS + 1];
        for (int i = 0;i<cl1s.length;i++){
            bl_count[cl1s[i]] ++;
        }
        //当前层最左边的叶子节点的码字的值
        int code = 0;
        //将对0的统计清空
        bl_count[0] = 0;
        //存储每一层最左边的叶子节点的码字
        int[] next_code = new int[MAX_BITS + 1];

        for (int i = 1; i < MAX_BITS + 1;i++){
            code = (code + bl_count[i-1]) << 1;
            next_code[i] = code;
        }

        HashMap<BitBuff,Integer> map_cll = new HashMap<>();

        for (int i = 0; i < cl1s.length ; i++ ){
            //n为第i项cll所表示的码字长度
            int n_clen = cl1s[i];
            if (n_clen != 0){
                if ( i <= 256 ){
                    //表示是literal
                    BitBuff huffman_code = getInfatingBinary(BitBuff.convert(next_code[n_clen]),n_clen);
                    map_cll.put(huffman_code,i);
                    next_code[n_clen] ++;
                }else {
                    //表示是length，需要按照分区编码还原
                    int[] length_group = LENGTH_GROUP_CODE[i-257];
                    for (int j = 0;j<length_group.length;j++){
                        int len = length_group[j];
                        BitBuff huffman_code = getInfatingBinary(BitBuff.convert(next_code[n_clen]),n_clen).append(getExtraBitsOfLength(len).reverse());
                        map_cll.put(huffman_code, len + 254 );
                    }
                    next_code[n_clen] ++;
                }
            }
        }
        return map_cll;
    }

    public static HashMap<BitBuff,Integer> getMapOfCL2(int[] cl2s){
        //cl1s中出现的最大值（huffman的高度）
        int MAX_BITS = getMaxValue(cl2s);
        //对huffman树每层叶子节点进行统计
        int[] bl_count = new int[MAX_BITS + 1];
        for (int i = 0;i<cl2s.length;i++){
            bl_count[cl2s[i]] ++;
        }
        //当前层最左边的叶子节点的码字的值
        int code = 0;
        //将对0的统计清空
        bl_count[0] = 0;
        //存储每一层最左边的叶子节点的码字
        int[] next_code = new int[MAX_BITS + 1];

        for (int i = 1; i < MAX_BITS + 1;i++){
            code = (code + bl_count[i-1]) << 1;
            next_code[i] = code;
        }

        HashMap<BitBuff,Integer> map_cl2 = new HashMap<>();

        for (int i = 0; i < cl2s.length ; i++ ){
            //n为第i项cll所表示的码字长度
            int n_clen = cl2s[i];
            if (n_clen != 0){
                int[] distance_group = DISTANCE_GROUP_CODE[i];
                for (int j = 0;j<distance_group.length;j++){
                    int distance = distance_group[j];
                    BitBuff huffman_code = getInfatingBinary(BitBuff.convert(next_code[n_clen]),n_clen).append(getExtraBitsOfDistance(distance).reverse());
                    map_cl2.put(huffman_code, distance );
                }
                next_code[n_clen] ++;
            }
        }
        return map_cl2;
    }

    private static BitBuff getInfatingBinary(BitBuff bitBuff,int huffman_code_len){
        //在bitBuff前面填充0，直到其位数为huffman_code_len
        int inflating_zeros = huffman_code_len - bitBuff.getBuffLength();
        bitBuff.insertBits(0,false,inflating_zeros);
        return bitBuff;
    }

    private static BitBuff getExtraBitsOfLength(int len){
        if (inRange(len,3,11)){
            return new BitBuff();
        }else if (inRange(len,11,19)){
            return BitBuff.convert((1-len%2));
        }else if (inRange(len,19,35)){
            return getInfatingBinary(BitBuff.convert((len-19)%4),2);
        }else if (inRange(len,35,97)){
            return getInfatingBinary(BitBuff.convert((len-35)%8),3);
        }else if (inRange(len,47,131)){
            return getInfatingBinary(BitBuff.convert((len-47)%16),4);
        }else if (inRange(len,131,258)){
            return getInfatingBinary(BitBuff.convert((len-131)%32),5);
        }
        return new BitBuff();
    }

    private static BitBuff getExtraBitsOfDistance(int len){
        if (inRange(len,1,5)){
            return new BitBuff();
        }else if (inRange(len,5,9)){
            return BitBuff.convert((1-len%2));
        }else if (inRange(len,9,17)){
            return getInfatingBinary(BitBuff.convert((len-9)%4),2);
        }else if (inRange(len,17,33)){
            return getInfatingBinary(BitBuff.convert((len-17)%8),3);
        }else if (inRange(len,33,65)){
            return getInfatingBinary(BitBuff.convert((len-33)%16),4);
        }else if (inRange(len,65,129)){
            return getInfatingBinary(BitBuff.convert((len-65)%32),5);
        }else if (inRange(len,129,257)){
            return getInfatingBinary(BitBuff.convert((len-129)%64),6);
        }else if (inRange(len,257,513)){
            return getInfatingBinary(BitBuff.convert((len-257)%128),7);
        }else if (inRange(len,513,1025)){
            return getInfatingBinary(BitBuff.convert((len-513)%256),8);
        }else if (inRange(len,2049,4097)){
            return getInfatingBinary(BitBuff.convert((len-2049)%1024),10);
        }else if (inRange(len,4097,8193)){
            return getInfatingBinary(BitBuff.convert((len-4097)%2048),11);
        }else if (inRange(len,8193,16385)){
            return getInfatingBinary(BitBuff.convert((len-8193)%4096),12);
        }else if (inRange(len,16385,32768)){
            return getInfatingBinary(BitBuff.convert((len-16385)%8192),13);
        }
        return new BitBuff();
    }



    private static boolean inRange(int num,int bot,int top){
        return num >= bot && num < top;
    }



    private static int getMaxValue(int[] values){
        int max = values[0];
        for (int i = 0;i<values.length;i++){
            if (max < values[i] ) max = values[i];
        }
        return max;
    }

    public static void printHuffmanTable(HashMap<BitBuff,Integer> map){
        for (BitBuff key : map.keySet()){
            System.out.println(String.format("%s -> %d",key.toString(),map.get(key)));
        }
    }

    public static void printHuffman1Table(HashMap<BitBuff,Integer> map){
        for (BitBuff key : map.keySet()){
            if (map.get(key) <256){
                System.out.println(String.format("%s -> %s",key.toString(),(char)map.get(key).intValue()));
            }else {
                System.out.println(String.format("%s -> %d",key.toString(),map.get(key)));
            }

        }
    }

}
