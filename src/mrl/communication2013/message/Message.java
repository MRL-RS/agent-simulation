package mrl.communication2013.message;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Aug 25, 2010
 * Time: 4:50:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Message {

    BitOperator bitOp;
    private int ptr = 0;
    int bitSize = 0;

    public Message(int size) {
        bitSize = size << 3;
        bitOp = new BitOperator(bitSize);
        setPtr(0);
    }

    public Message(byte[] bytes) {
        bitOp = new BitOperator(bytes);
        bitSize = bytes.length << 3;
        setPtr(bitSize);
    }

    public void equal(Message m) {
        this.setPtr(m.ptr);
        this.bitSize = m.bitSize;
        this.bitOp.equal(m.bitOp);
    }

    public int read(int size, boolean moveOnByte) {
        if (moveOnByte) {
            setPtrToStartByte();
        }
        int res = bitOp.getValue(getPtr(), size);
        setPtr(getPtr() + size);
        return res;
    }

    private void setPtrToStartByte() {
        int ex = (ptr) % 8;
        if (ex > 0) {
            setPtr(getPtr() + 8 - ex);
        }
    }

    public boolean write(int value, int size) {
        String val = Integer.toBinaryString(value);
        if (val.length() > size)
            return false;
        bitOp.setValue(value, getPtr(), size);
        setPtr(getPtr() + size);
        return true;
    }

    public int getPtr() {
        return ptr;
    }

    public void resetPtr() {
        setPtr(0);
    }

    public int getBitSize() {
        return bitSize;
    }

    public byte[] getBytes() {
        //Mostafa Movahedi
        byte[] ret = new byte[(int) Math.ceil((double) getPtr() / 8)];
        int toIdx = ret.length << 3;
        byte b = 0;
        for (int i = 0; i < toIdx; ++i) {
            if (bitOp.getBit(i) == 1) {
                int temp = 7 - i % 8;
                b += 1 << temp;
            }
            if (i % 8 == 7) {
                ret[i >> 3] = b;
                b = 0;
            }
        }
        return ret;
        //Mostafa Movahedi
//        return bitOp.getByte();
    }

    public BitOperator getBitOp() {
        return bitOp;
    }

    public void setPtr(int ptr) {
        this.ptr = ptr;
    }
}
