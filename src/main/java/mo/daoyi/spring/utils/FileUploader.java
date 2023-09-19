package mo.daoyi.spring.utils;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FileUploader {
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB
    private static final int CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String UPLOAD_URL = "http://localhost/upload.php";
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_KEY_PREFIX = "fileupload_";

    public static void main(String[] args) throws Exception {
        String filePath = "/path/to/file";
        File file = new File(filePath);
        String fileId = getFileId(file);
        JedisUtil jedisPoolConfig = new JedisUtil();
//        Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT);

        if (jedisPoolConfig.exists(REDIS_KEY_PREFIX + fileId)) {
            // File already uploaded, return file ID
            System.out.println("File already uploaded. File ID: " + fileId);
            return;
        }

        // Calculate MD5 checksum
        String md5 = calculateMD5(file);

        // Check if file already exists on server
        Map<String, String> params = new HashMap<>();
        params.put("md5", md5);
        String response = sendHttpRequest(UPLOAD_URL + "?action=check", params);
        if (!response.equals("0")) {
            // File already exists, return file ID
            jedisPoolConfig.set(REDIS_KEY_PREFIX + fileId, response);
            System.out.println("File already exists. File ID: " + response);
            return;
        }

        // Upload file
        String uploadId = UUID.randomUUID().toString();
        int chunkCount = (int) Math.ceil((double) file.length() / CHUNK_SIZE);
        for (int i = 0; i < chunkCount; i++) {
            int chunkIndex = i + 1;
            long start = i * CHUNK_SIZE;
            long end = Math.min((i + 1) * CHUNK_SIZE, file.length());
            long size = end - start;
            String chunkId = uploadId + "_" + chunkIndex;
            String chunkKey = REDIS_KEY_PREFIX + chunkId;

            if (jedisPoolConfig.exists(chunkKey)) {
                // Chunk already uploaded, skip
                continue;
            }

            // Read chunk data
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = 0;
            try (InputStream input = new FileInputStream(file)) {
                input.skip(start);
                bytesRead = input.read(buffer, 0, (int) size);
            }

            // Upload chunk
            params.clear();
            params.put("md5", md5);
            params.put("chunkIndex", String.valueOf(chunkIndex));
            params.put("chunkCount", String.valueOf(chunkCount));
            params.put("uploadId", uploadId);
            params.put("chunkSize", String.valueOf(size));
            params.put("chunkMd5", calculateMD5(buffer, 0, bytesRead));
            String responseCode = sendHttpRequest(UPLOAD_URL + "?action=upload", params, buffer, 0, bytesRead);

            if (!responseCode.equals("200")) {
                // Upload failed, retry later
                jedisPoolConfig.delete(chunkKey);
                System.out.println("Upload failed. Chunk ID: " + chunkId);
                return;
            }

            // Mark chunk as uploaded
            jedisPoolConfig.set(chunkKey, "1");
        }

        // Complete upload
        params.clear();
        params.put("md5", md5);
        params.put("uploadId", uploadId);
        response = sendHttpRequest(UPLOAD_URL + "?action=complete", params);

        if (!response.equals("0")) {
            // Upload failed, retry later
            jedisPoolConfig.delete(REDIS_KEY_PREFIX + uploadId);
            System.out.println("Upload failed. Upload ID: " + uploadId);
            return;
        }

        // Mark file as uploaded
        jedisPoolConfig.set(REDIS_KEY_PREFIX + fileId, uploadId);
        System.out.println("Upload successful. File ID: " + fileId);
    }

    private static String getFileId(File file) {
        return file.getName() + "_" + file.length();
    }

    private static String calculateMD5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = 0;
            while ((bytesRead = input.read(buffer, 0, BUFFER_SIZE)) > 0) {
                md.update(buffer, 0, bytesRead);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String calculateMD5(byte[] data, int offset, int length) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data, offset, length);
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String sendHttpRequest(String url, Map<String, String> params) throws Exception {
        return sendHttpRequest(url, params, null, 0, 0);
    }

    private static String sendHttpRequest(String url, Map<String, String> params, byte[] data, int offset, int length) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        try (OutputStream output = conn.getOutputStream()) {
            output.write(sb.toString().getBytes());
            if (data != null && length > 0) {
                output.write(data, offset, length);
            }
        }

        StringBuilder response = new StringBuilder();
        try (InputStream input = conn.getInputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = 0;
            while ((bytesRead = input.read(buffer, 0, BUFFER_SIZE)) > 0) {
                response.append(new String(buffer, 0, bytesRead));
            }
        }

        return conn.getResponseCode() + response.toString();
    }
}
