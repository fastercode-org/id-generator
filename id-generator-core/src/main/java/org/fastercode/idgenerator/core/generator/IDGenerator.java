package org.fastercode.idgenerator.core.generator;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.fastercode.idgenerator.core.exception.IDGeneratorException;
import org.fastercode.idgenerator.core.util.IPUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 分布式订单号生成器
 *
 * @author huyaolong
 */
@Slf4j
public class IDGenerator {
    private static final ThreadLocal<Map<String, SimpleDateFormat>> simpleDateFormatThreadLocal = ThreadLocal.withInitial(ConcurrentHashMap::new);
    private static final String DATE_FMT = "yyMMddHHmmss";
    private static final long ipSuffix;

    static {
        String ip = IPUtil.getLocalAddress();
        if (Strings.isNullOrEmpty(ip)) {
            ipSuffix = 0;
        } else {
            int pos = 0;
            for (int i = 0; i < ip.length(); i++) {
                if (ip.charAt(i) == '.') {
                    pos = i;
                }
            }
            ipSuffix = Integer.parseInt(ip.substring(pos + 1));
        }
    }

    private static final long EXTRA_DATA_BITS = 9L;
    private static final long EXTRA_DATA_MAX = (1L << EXTRA_DATA_BITS) - 1L;
    private static final long EXTRA_DATA_LEFT = 0L;
    private static final long EXTRA_DATA_MASK = ((1L << EXTRA_DATA_BITS) - 1L) << EXTRA_DATA_LEFT;

    private static final long SEQUENCE_BITS = 12L;
    private static final long SEQUENCE_MAX = (1L << SEQUENCE_BITS) - 1L;
    private static final long SEQUENCE_LEFT = EXTRA_DATA_BITS + EXTRA_DATA_LEFT;
    private static final long SEQUENCE_MASK = ((1L << SEQUENCE_BITS) - 1L) << SEQUENCE_LEFT;

    private static final long WORKER_ID_BITS = 10L;
    private static final long WORKER_ID_LEFT = SEQUENCE_BITS + SEQUENCE_LEFT;
    private static final long WORKER_ID_MASK = ((1L << WORKER_ID_BITS) - 1L) << WORKER_ID_LEFT;

    private static final long TIMESTAMP_BITS = 32L;
    private static final long TIMESTAMP_LEFT = WORKER_ID_BITS + WORKER_ID_LEFT;

    private final LongSupplier workerIdSupplier;

    private long lastSeconds = 0L;
    private long lastTimeMillis = 0L;
    private long sequence = 0L;

    /**
     * 实例化一个ID生成器, workerId=IP后三位
     */
    public IDGenerator() {
        this.workerIdSupplier = () -> ipSuffix;
    }

    /**
     * 实例化一个ID生成器, workerId=自定义
     */
    public IDGenerator(LongSupplier workerIdSupplier) {
        this.workerIdSupplier = workerIdSupplier;
    }

    /**
     * 生成一个分布式 ID
     */
    public ID generate() {
        return generate(0);
    }

    /**
     * 生成一个分布式 ID, 且包含 extraData
     */
    public synchronized ID generate(long extraData) {
        if (extraData > EXTRA_DATA_MAX || extraData < 0) {
            throw new IDGeneratorException("extraData 合法范围是: [0," + EXTRA_DATA_MAX + "]");
        }

        long workerId = workerIdSupplier.getAsLong();
        if (workerId > 999 || workerId < 0L) {
            throw new IDGeneratorException("workerId 合法范围是: [0,999]");
        }

        long currentTimeMillis = System.currentTimeMillis();
        long currentSeconds = currentTimeMillis / 1000L;

        // 优化时钟回拨
        if (lastSeconds > currentSeconds) {
            long backMillis = lastTimeMillis - currentTimeMillis;
            if (backMillis <= 500) {
                log.warn("机器时钟已回拨, 等待{{}}ms", backMillis);
                try {
                    Thread.sleep(backMillis);
                } catch (InterruptedException ignore) {
                }
                // 重置时间值
                currentTimeMillis = System.currentTimeMillis();
                currentSeconds = currentTimeMillis / 1000L;
            } else {
                throw new IDGeneratorException("机器时钟回拨{" + backMillis + "}ms超过500ms, 订单号生成失败.");
            }
        }

        // 请求处在同一秒, sequence++
        if (currentSeconds == lastSeconds) {
            sequence++;
            if (sequence > SEQUENCE_MAX) {
                throw new IDGeneratorException("同一秒内生成id超过{" + SEQUENCE_MAX + "}个, 当前{" + sequence + "}, 订单号生成失败.");
            }
        } else {
            // 请求已在下一秒, sequence归0
            lastSeconds = currentSeconds;
            lastTimeMillis = currentTimeMillis;
            sequence = 0L;
        }

        ID id = new ID();
        Date now = new Date(currentSeconds * 1000L);
        id.setCreateDate(now);
        id.setLong64((currentSeconds << TIMESTAMP_LEFT) | (workerId << WORKER_ID_LEFT) | (sequence << SEQUENCE_LEFT) | (extraData << EXTRA_DATA_LEFT));
        id.setStr(dateFormat(DATE_FMT).format(now) + String.format("%03d", workerId) + String.format("%03d", sequence));
        id.setStrWithExtraData(id.getStr() + String.format("%03d", extraData));
        return id;
    }

    /**
     * 从id反解出 workerID
     */
    public static long decodeWorkerIdFromId(final long id) {
        return (id & WORKER_ID_MASK) >> WORKER_ID_LEFT;
    }

    /**
     * 从id反解出 extraData
     */
    public static long decodeExtraDataFromId(final long id) {
        return (id & EXTRA_DATA_MASK) >> EXTRA_DATA_LEFT;
    }

    /**
     * 从id反解出 创建时间
     */
    public static Date decodeCreateDateFromId(final long id) {
        if (id < (1L << 32L)) {
            return null;
        }
        return new Date((id >> TIMESTAMP_LEFT) * 1000L);
    }

    /**
     * 从str反解出 创建时间
     */
    public static Date decodeCreateDateFromNo(final String no) {
        try {
            return dateFormat(DATE_FMT).parse(decodeCreateDateStrFromNo(no));
        } catch (ParseException e) {
            log.error("从order_no反解创建时间异常.", e);
            return null;
        }
    }

    /**
     * 从str反解出 创建时间字符串
     */
    public static String decodeCreateDateStrFromNo(final String no) {
        if (no == null || no.length() < 12) {
            return null;
        }

        int pos = 0;
        for (int i = 0; i < no.length(); i++) {
            int c = no.charAt(i);
            if (c >= '0' && c <= '9') {
                pos = i;
                break;
            }
        }

        if (no.length() - pos < 12) {
            return null;
        }

        return no.substring(pos, pos + 12);
    }

    private static SimpleDateFormat dateFormat(String fmt) {
        Map<String, SimpleDateFormat> formatMap = simpleDateFormatThreadLocal.get();
        if (formatMap.containsKey(fmt)) {
            return formatMap.get(fmt);
        } else {
            SimpleDateFormat format = new SimpleDateFormat(fmt);
            formatMap.put(fmt, format);
            return format;
        }
    }

}
