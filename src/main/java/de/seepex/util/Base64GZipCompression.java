package de.seepex.util;

import java.io.*;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Base64GZipCompression {

    public static byte[] compress(String data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data.getBytes());
        gzip.close();
        byte[] compressed = bos.toByteArray();
        bos.close();
        //return compressed;
        return Base64.getEncoder().encode(compressed);
    }

    public static String decompress(byte[] compressed) throws IOException {
        compressed = Base64.getDecoder().decode(compressed);
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(bis);
        BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        gis.close();
        bis.close();
        return sb.toString();
    }


}
