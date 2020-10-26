package org.fastercode.idgenerator.core.util;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * @author huyaolong
 */
@Slf4j
public class FileUtil {

    private static final String CHARSETS = "UTF8";

    public static boolean writeFileContent(String filename, String content) {
        if (Strings.isNullOrEmpty(filename)) {
            throw new RuntimeException("文件名不能为空");
        }

        String filenameTmp = filename + ".tmp." + System.currentTimeMillis();
        File fileTmp = new File(filenameTmp);

        boolean succ = true;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(fileTmp, "rw");
            byte[] bytes = content.getBytes(CHARSETS);
            raf.write(bytes, 0, bytes.length);

        } catch (Exception ignored) {
            succ = false;
        } finally {
            try {
                if (raf != null)
                    raf.close();
            } catch (IOException ignored) {
                succ = false;
            }
        }

        if (!fileTmp.renameTo(new File(filename))) {
            succ = false;
        }

        try {
            fileTmp.delete();
        } catch (Exception ignore) {
        }

        if (succ) {
            log.info("文件写入成功: [{}]", filename);
        } else {
            log.warn("文件写入失败: [{}]", filename);
        }

        return succ;
    }

    public static String readFileContent(String filename) {
        if (Strings.isNullOrEmpty(filename)) {
            return null;
        }

        File file = new File(filename);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            return null;
        }

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            long len = raf.length();
            if (len == 0L) {
                return "";
            }

            byte[] bytes = new byte[(int) len];
            raf.read(bytes, 0, (int) len);
            return new String(bytes, CHARSETS);

        } catch (IOException ignored) {
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }


    /**
     * 把文件内容读入到String
     *
     * @param file 文件句柄
     * @return 文件内容
     * @throws IOException
     */
    public static String readFileToStr(File file) throws IOException {
        byte[] bytes = readFileToBytes(file);

        return new String(bytes, CHARSETS);
    }

    /**
     * 将字符串写入文件
     *
     * @param str  字符串
     * @param file 文件句柄
     * @throws IOException
     */
    public static void writeStrToFile(String str, File file) throws IOException {
        byte[] bytes = str.getBytes(CHARSETS);

        writeBytesToFile(bytes, file);
    }

    /**
     * 将文件读出为字节数组
     *
     * @param file 文件句柄
     * @return 文件内容对应的字节数组
     * @throws IOException
     */
    private static byte[] readFileToBytes(File file) throws IOException {
        int size = (int) file.length();
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            byte[] bytes = new byte[size];
            fin.read(bytes);
            return bytes;
        } catch (Exception e) {
            if ((e instanceof IOException)) {
                throw ((IOException) e);
            }
            log.warn(e.getMessage(), e);
            return null;
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 将字节数组写入文件
     *
     * @param bytes 字节数组
     * @param file  文件句柄
     * @throws IOException
     */
    private static void writeBytesToFile(byte[] bytes, File file) throws IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            fout.write(bytes);
        } catch (Exception e) {
            if ((e instanceof IOException)) {
                throw ((IOException) e);
            }
            log.warn(e.getMessage(), e);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }
    }
}
