package com.tencent.mobile.main.com.tencent.mobile.zip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

/**
 * Created by realhe on 2016/7/21.
 */
public class MyZipEntry implements MyZipConstants{
    private int version = -1;              //    version needed to extract       2 bytes
    private int flag = -1;                 //    general purpose bit flag        2 bytes
    private int method = -1;               //    compression method              2 bytes
    private int time = -1;                 //    last mod file time              2 bytes
    private int date = -1;                 //    last mod file date              2 bytes
    private long modifiedTime;             //    include time and date(last modified)
    private long crc = -1;                 //    crc-32                          4 bytes
    private long compressedSize = -1;      //    compressed size                 4 bytes
    private long uncompressedSize = -1;    //    uncompressed size               4 bytes
    private int fileNameLength = -1;       //    file name length                2 bytes
    private int extraFieldLength = -1;     //    extra file length               2 bytes
    private long startPosition = -1;       //    total length of data and local file header


    private File file;
    private UnPackHelper unPackHelper;



    MyZipEntry(byte[] bytes, File file)throws IllegalArgumentException,ZipFormatException{
        if (bytes.length != LOCHDR)
            throw new IllegalArgumentException("invalid entry local file header");
        if (MyZipUtils.get32(bytes,0) != LOCSIG)
            throw new ZipFormatException();
        setVersion(MyZipUtils.get16(bytes,LOCVER));
        setFlag(MyZipUtils.get16(bytes,LOCFLG));
        setMethod(MyZipUtils.get16(bytes,LOCHOW));
        setTime(MyZipUtils.get16(bytes,LOCTIM));
        setDate(MyZipUtils.get16(bytes,LOCTIM+2));
        setModifiedTime(MyZipUtils.get32(bytes,LOCTIM));
        setCrc(MyZipUtils.get32(bytes,LOCCRC));
        setCompressedSize(MyZipUtils.get32(bytes,LOCSIZ));
        setUncompressedSize(MyZipUtils.get32(bytes,LOCLEN));
        setFileNameLength(MyZipUtils.get16(bytes,LOCNAM));
        setExtraFieldLength(MyZipUtils.get16(bytes,LOCEXT));
        this.file = file;
    }

    public void unpack()throws IOException,UnpackException{
        if (unPackHelper == null)
            unPackHelper = new UnPackHelper(file,startPosition + getHeaderLength(),compressedSize);
        unPackHelper.unpack();
    }

    public void print(){
        System.out.println("version : " + version);
        System.out.println("flag : " + flag);
        System.out.println("method : " + method);
        System.out.println("time : " + time);
        System.out.println("date : " + date);
        System.out.println("lastModifiedTime" +  MyZipUtils.dosToJavaTime(modifiedTime));
        System.out.println("crc : " + Long.toHexString(crc));
        System.out.println("compressedSize : " + compressedSize);
        System.out.println("uncompressedSize : " + uncompressedSize);
        System.out.println("fileNameLength : " + fileNameLength);
        System.out.println("extraFieldLength : " + extraFieldLength);
        System.out.println("startPosition : " + Long.toHexString(startPosition));
    }

    public void printValue(){
        System.out.println("version : " + version);
        System.out.println("flag : " + flag);
        System.out.println("method : " + method);
        System.out.println("time : " + time);
        System.out.println("date : " + date);
        System.out.println("lastModifiedTime : " + MyZipUtils.dosToJavaTime(modifiedTime));
        System.out.println("crc : " + crc);
        System.out.println("compressedSize : " + compressedSize);
        System.out.println("uncompressedSize : " + uncompressedSize);
        System.out.println("fileNameLength : " + fileNameLength);
        System.out.println("extraFieldLength : " + extraFieldLength);
        System.out.println("startPosition : " + startPosition);
    }



    public FileTime getLastModifiedTime(){
        if (modifiedTime == -1)
            return null;
        return FileTime.from(modifiedTime, TimeUnit.MILLISECONDS);
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime & 0xffffffffL;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    long getLength(){
        /**TODO  当flag不为0时此处还有Data descriptor:
        crc-32                          4 bytes
        compressed size                 4 bytes(8 bytes for ZIP64(tm) format)
        uncompressed size               4 bytes(8 bytes for ZIP64(tm) format)
         */
        return LOCHDR
                + getFileNameLength()
                + getExtraFieldLength()
                + getCompressedSize();
    }

    long getHeaderLength(){
        return LOCHDR
                + getFileNameLength()
                + getExtraFieldLength();
    }

    public long getStartPosition() {
        return startPosition;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time & 0x0000FFFF;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date & 0x0000FFFF;
    }

    public long getCrc() {
        return crc;
    }

    public void setCrc(long crc) {
        this.crc = crc & 0x00000000FFFFFFFFL;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize & 0x00000000FFFFFFFFL;
    }

    public long getUncompressedSize() {
        return uncompressedSize;
    }

    public void setUncompressedSize(long uncompressedSize) {
        this.uncompressedSize = uncompressedSize & 0x00000000FFFFFFFFL;
    }

    public int getFileNameLength() {
        return fileNameLength;
    }

    public void setFileNameLength(int fileNameLength) {
        this.fileNameLength = fileNameLength & 0x0000FFFF;
    }

    public int getExtraFieldLength() {
        return extraFieldLength;
    }

    public void setExtraFieldLength(int extraFieldLength) {
        this.extraFieldLength = extraFieldLength & 0x0000FFFF;
    }

    public void setStartPosition(long startPosition) {
        this.startPosition = startPosition;
    }
}
