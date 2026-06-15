package com.wo.common.constant;

public class WorkOrderConstant {

    public static final String ORDER_NO_PREFIX = "WO-";

    public static final int AI_MEMORY_WINDOW_SIZE = 20;
    public static final int AI_MEMORY_SUMMARIZE_THRESHOLD = 15;
    public static final int AI_MAX_TOKEN_CONTEXT = 6000;

    public static final int RAG_DEFAULT_TOP_K = 5;
    public static final int RAG_CHUNK_SIZE = 512;
    public static final int RAG_CHUNK_OVERLAP = 128;
    public static final int RAG_EMBEDDING_DIM = 1024;

    public static final int AGENT_MAX_ITERATIONS = 10;

    private WorkOrderConstant() {}
}
