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

    public static BitBuff getInfatingBinary(BitBuff bitBuff,int huffman_code_len){
        int inflating_zeros = huffman_code_len - bitBuff.getBuffLength();
        bitBuff.insertBits(0,false,inflating_zeros);
        return bitBuff;
    }

    public static int getMaxValue(int[] values){
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


}
