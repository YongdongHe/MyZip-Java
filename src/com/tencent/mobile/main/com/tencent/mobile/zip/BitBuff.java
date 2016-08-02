package com.tencent.mobile.main.com.tencent.mobile.zip;

import java.util.BitSet;

/**
 * Created by realhe on 2016/8/1.
 */
public class BitBuff extends BitSet {
    private int buffLength = 0;
    public BitBuff() {
    }

    public BitBuff(int nbits) {
        super(nbits);
        this.buffLength = nbits;
    }

    public int getBuffLength(){
        return buffLength;
    }

    public BitBuff append(boolean bitValue){
        this.set(buffLength,bitValue);
        buffLength ++;
        return this;
    }

    public BitBuff append(boolean[] bitValues){
        for(boolean value : bitValues){
            append(value);
        }
        return this;
    }

    //将buff中的所有bit存放顺序倒置
    public BitBuff reverse(){
        BitBuff temp = new BitBuff();
        for (int i = 0 ; i<buffLength ; i++){
            temp.append(this.get(i));
        }
        clear();
        for (int i = buffLength -1 ;i >= 0 ; i--){
            this.append(temp.get(i));
        }
        return this;
    }

    @Override
    public void clear() {
        super.clear();
        buffLength = 0;
    }

    //传入的value为0，则append一个false，否则传入一个1
    public BitBuff append(int value){
        append(!(value==0));
        return this;
    }

    public int getValue(){
        int value = 0;
        int factor = 0x1;
        for (int i=buffLength-1;i>=0;i++){
            if (this.get(i))value+=factor;
            factor <<= 1;
        }
        return value;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < this.getBuffLength() ;i++){
            if (this.get(i))
            {
                sb.append("1");
            }
            else{
                sb.append("0");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
