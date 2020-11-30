package pers.pandora.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;


public class IdWorker {

    private static Logger logger = LogManager.getLogger(IdWorker.class);
    //snowflake 改版
    protected long epoch = 1288834974657L;

    protected long workerIdBits = 10L;

    protected long maxWorkerId = -1L ^ (-1L << workerIdBits);

    protected long sequenceBits = 11L;

    protected long workerIdShift = sequenceBits;

    protected long timestampLeftShift = sequenceBits + workerIdBits;

    protected long sequenceMask = -1L ^ (-1L << sequenceBits);

    protected long lastMillis = -1L;

    protected long workerId;

    protected long sequence = 0L;

    public IdWorker() {
        //初始随机数
        this.workerId = checkWorkerId((System.identityHashCode(this) ^ System.currentTimeMillis()) & maxWorkerId);
        logger.debug("worker starting. timestamp left shift " + LOG.LOG_PRE + ", worker id " + LOG.LOG_PRE, timestampLeftShift, workerId);
    }

    private long checkWorkerId(long workerId) {
        // sanity check for workerId
        if (workerId > maxWorkerId || workerId < 0) {
            int rand = new SecureRandom().nextInt((int) maxWorkerId + 1);
            logger.warn("worker Id can't be greater than " + LOG.LOG_PRE + "or less than 0, use a random " + LOG.LOG_PRE, maxWorkerId, rand);
            return rand;
        }

        return workerId;
    }

    public synchronized long nextId() {
        long timestamp = millisGen();

        if (timestamp < lastMillis) {
            logger.error("clock is moving backwards.  Rejecting requests until " + LOG.LOG_PRE, lastMillis);
            return maxWorkerId;
        }

        if (lastMillis == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0)
                timestamp = tilNextMillis(lastMillis);
        } else {
            sequence = 0;
        }

        lastMillis = timestamp;
        long diff = timestamp - epoch;
        return (diff << timestampLeftShift) |
                (workerId << workerIdShift) |
                sequence;
    }

    protected long tilNextMillis(long lastMillis) {
        long millis = millisGen();
        while (millis <= lastMillis)
            millis = millisGen();

        return millis;
    }

    protected long millisGen() {
        return System.currentTimeMillis();
    }

    public long getLastMillis() {
        return lastMillis;
    }

    public long getWorkerId() {
        return workerId;
    }

    /**
     * 一天最大毫秒86400000，最大占用27比特
     * 27+10+11=48位 最大值281474976710655(15字)，YK0XXHZ827(10字)
     * 6位(YYMMDD)+15位，共21位
     *
     * @return 固定21位数字字符串
     */
    public static final String NEXT_FORMAT = "%014d";

    public static final String DATE_FORMAT = "yyMMdd";

    public static final char PADDING_CHAR = '0';

    public String next() {
        long id = nextId();
        String yyMMdd = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        return yyMMdd + String.format(NEXT_FORMAT, id);
    }


    /**
     * 返回固定16位的字母数字混编的字符串。
     */
    public String nextShort() {
        long id = nextId();
        String yyMMdd = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        return padLeft(encode(id), 10, PADDING_CHAR) + yyMMdd;
    }

    public static String encode(long num) {
        return encode(num, defaultRange);
    }

    public static String encode(long num, String symbols) {
        final int B = symbols.length();
        StringBuilder sb = new StringBuilder();
        while (num != 0) {
            sb.append(symbols.charAt((int) (num % B)));
            num /= B;
        }
        return sb.reverse().toString();
    }

    // all un-clearly-recognized letters are skiped.
    static String defaultRange = "0123456789ABCDFGHKMNPRSTWXYZ";

    public static String padLeft(String str, int size, char padChar) {
        if (str.length() == size) return str;
        StringBuilder s = new StringBuilder();
        if (str.length() > size) {
            s.append(str);
            for (int i = str.length() - size, j = 0; i > 0; i--) {
                int k = new Random().nextInt(s.length() - j++);
                s.delete(k, k + 1);
            }
        } else {
            for (int i = size - str.length(); i > 0; --i) {
                s.append(padChar);
            }
            s.append(str);
        }

        return s.toString();
    }

    //SessionID生成器(53位:16+1+36(32+4)) 750+ ms 延迟
    public String nextSessionID() {
//        long start = System.currentTimeMillis();
        return nextShort() + "_" + UUID.randomUUID().toString().toUpperCase();
//        long end = System.currentTimeMillis();
//        System.out.println(head+"_"+tail+":"+(end-start)+"ms");
    }

}
