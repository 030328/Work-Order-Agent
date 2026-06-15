package com.wo.agent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档分块服务
 * 使用递归字符分割策略将长文本切分为适合向量化的小块
 */
@Slf4j
@Service
public class DocumentChunker {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 50;

    /**
     * 分块策略分隔符（按优先级）
     */
    private static final String[] SEPARATORS = {"\n\n", "\n", "。", ".", "！", "!", "？", "?", "；", ";"};

    /**
     * 将文本分块
     *
     * @param text      原始文本
     * @param chunkSize 每块最大字符数
     * @param overlap   块间重叠字符数
     * @return 分块后的文本列表
     */
    public List<String> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        if (chunkSize <= 0) {
            chunkSize = DEFAULT_CHUNK_SIZE;
        }
        if (overlap < 0) {
            overlap = DEFAULT_OVERLAP;
        }

        List<String> chunks = new ArrayList<>();
        recursiveSplit(text, chunkSize, overlap, 0, chunks);

        log.debug("Document chunked: {} chars -> {} chunks", text.length(), chunks.size());
        return chunks;
    }

    /**
     * 使用默认参数分块
     */
    public List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * 递归分割文本
     */
    private void recursiveSplit(String text, int chunkSize, int overlap, int separatorIndex,
                                 List<String> chunks) {
        if (text.length() <= chunkSize) {
            chunks.add(text.trim());
            return;
        }

        // 选择分隔符
        String separator = separatorIndex < SEPARATORS.length
                ? SEPARATORS[separatorIndex]
                : "";

        if (separator.isEmpty()) {
            // 没有更多分隔符，强制按字符数截断
            chunks.add(text.substring(0, chunkSize).trim());
            if (text.length() > chunkSize) {
                recursiveSplit(text.substring(chunkSize - overlap), chunkSize, overlap,
                        separatorIndex, chunks);
            }
            return;
        }

        // 按分隔符分割
        String[] parts = text.split(java.util.regex.Pattern.quote(separator), -1);
        StringBuilder currentChunk = new StringBuilder();

        for (String part : parts) {
            String candidate = currentChunk.length() == 0
                    ? part
                    : currentChunk + separator + part;

            if (candidate.length() <= chunkSize) {
                currentChunk.append(currentChunk.length() == 0 ? part : separator + part);
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }

                // 如果单个 part 超过 chunkSize，用下一级分隔符继续分割
                if (part.length() > chunkSize) {
                    recursiveSplit(part, chunkSize, overlap, separatorIndex + 1, chunks);
                    currentChunk = new StringBuilder();
                } else {
                    // 处理重叠
                    String lastChunk = chunks.isEmpty() ? "" : chunks.get(chunks.size() - 1);
                    if (overlap > 0 && lastChunk.length() > overlap) {
                        currentChunk = new StringBuilder(
                                lastChunk.substring(lastChunk.length() - overlap) + separator + part);
                    } else {
                        currentChunk = new StringBuilder(part);
                    }
                }
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
    }
}
