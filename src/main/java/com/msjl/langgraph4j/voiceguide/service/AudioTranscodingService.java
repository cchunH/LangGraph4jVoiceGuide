package com.msjl.langgraph4j.voiceguide.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class AudioTranscodingService {

    private static final int MAX_ERROR_BYTES = 16 * 1024;
    private static final long TRANSCODE_TIMEOUT_SECONDS = 45L;

    public byte[] toPcm16Mono16k(byte[] source, String sourceFormat) {
        if (source == null || source.length == 0) {
            throw new IllegalArgumentException("Audio payload is empty");
        }
        String normalizedFormat = normalizeFormat(sourceFormat);
        if ("pcm".equals(normalizedFormat)) {
            return source;
        }

        Process process = null;
        try {
            process = new ProcessBuilder(
                    "ffmpeg",
                    "-hide_banner",
                    "-loglevel", "error",
                    "-f", normalizedFormat,
                    "-i", "pipe:0",
                    "-ac", "1",
                    "-ar", "16000",
                    "-f", "s16le",
                    "-acodec", "pcm_s16le",
                    "pipe:1"
            ).start();

            Process runningProcess = process;
            CompletableFuture<byte[]> outputFuture = CompletableFuture.supplyAsync(
                    () -> readAll(runningProcess.getInputStream(), Integer.MAX_VALUE)
            );
            CompletableFuture<byte[]> errorFuture = CompletableFuture.supplyAsync(
                    () -> readAll(runningProcess.getErrorStream(), MAX_ERROR_BYTES)
            );

            try (OutputStream input = process.getOutputStream()) {
                input.write(source);
            }

            if (!process.waitFor(TRANSCODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Audio transcoding timed out");
            }

            byte[] output = outputFuture.get(5, TimeUnit.SECONDS);
            byte[] errorOutput = errorFuture.get(5, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Audio transcoding failed: " + new String(errorOutput).trim());
            }
            if (output.length == 0) {
                throw new IllegalStateException("Audio transcoding produced no PCM data");
            }
            return output;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Audio transcoding was interrupted", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Audio transcoding failed", ex);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private String normalizeFormat(String sourceFormat) {
        String normalized = sourceFormat == null ? "" : sourceFormat.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "mp3", "mpeg" -> "mp3";
            case "aac" -> "aac";
            case "m4a", "mp4" -> "mp4";
            case "wav", "wave" -> "wav";
            case "pcm", "s16le" -> "pcm";
            default -> throw new IllegalArgumentException("Unsupported audio format: " + sourceFormat);
        };
    }

    private byte[] readAll(InputStream input, int maxBytes) {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                int remaining = maxBytes - total;
                if (remaining <= 0) {
                    break;
                }
                int accepted = Math.min(read, remaining);
                output.write(buffer, 0, accepted);
                total += accepted;
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read transcoder output", ex);
        }
    }
}
