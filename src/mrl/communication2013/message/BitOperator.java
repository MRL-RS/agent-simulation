package mrl.communication2013.message;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Aug 25, 2010
 * Time: 3:32:07 PM
 * To change this template use File | Settings | File Templates.
 */

//this class use for work with bits of Bytes to compress Message size
public class BitOperator {
    byte[] b = {0};
    int len = 0;
    int bytes = 0;

    public BitOperator(int len) {
        this.len = len;
        bytes = len >> 3;
        b = new byte[bytes];
        for (int i = 0; i < bytes; i++)
            b[i] = 0;
    }

    public BitOperator(byte[] bytes) {
        b = new byte[bytes.length];
        this.len = bytes.length << 3;
        this.bytes = bytes.length;
        System.arraycopy(bytes, 0, b, 0, this.bytes);
    }

    public boolean setBit(int index) {
        if (index >= len)
            return false;

        int byteNum = index >> 3;
        int bitNum = index & 7;
        this.b[byteNum] |= 1 << (7 - bitNum);
        return true;
    }

    private void flipBit(int index) {
        if (index > len)
            return;

        int byteNum = index >> 3;
        int bitNum = index & 7;
        b[byteNum] ^= 1 << (7 - bitNum);
    }

    public int getBit(int index) {
        if (index >= len)
            return 0;

        int byteNum = index >> 3;
        int bitNum = index & 7;

        int mask = 1 << (7 - bitNum);
        int temp = mask & b[byteNum];
        return temp >> (7 - bitNum);
    }

    public void setValue(int value, int start, int size) {
        String val = Integer.toBinaryString(value);
        int dif = size - val.length();
        char[] c = val.toCharArray();
        byte b[] = new byte[size];
        for (int i = 0; i < c.length; i++) {
            b[i + dif] = (byte) (c[i] - '0');
        }
        for (int i = 0; i < size; i++) {
            if (b[i] == 1) {
                setBit(i + start);
            }
        }
    }

    public int getValue(int start, int size) {
        int val = 0;
        for (int i = 0; i < size; i++) {
            val |= getBit(i + start);
            val <<= 1;
        }
        val >>= 1;
        val &= 0x7FFFFFFF; //todo: :D
        return val;
    }

    public byte[] getByte() {
        return b;
    }

    public void print() {
        for (int i = 0; i < len; i++)
            System.out.print(getBit(i));
    }

    public String getBits() {
        String s = "";
        for (int i = 0; i < len; i++)
            s += String.valueOf(getBit(i));
        return s;
    }

    public void equal(BitOperator bitOp) {
        this.bytes = bitOp.bytes;
        this.len = bitOp.len;
        System.arraycopy(bitOp.b, 0, this.b, 0, bitOp.b.length);
    }
}