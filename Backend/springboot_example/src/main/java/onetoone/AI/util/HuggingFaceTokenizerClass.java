// src/main/java/onetoone/AI/util/HuggingFaceTokenizer.java
package onetoone.AI.util;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OrtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HuggingFaceTokenizerClass {
    private static final Logger log = LoggerFactory.getLogger(HuggingFaceTokenizer.class);

    private final HuggingFaceTokenizer tokenizer;
    private static final String MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2";

    public HuggingFaceTokenizerClass() throws Exception {
        try {
            log.info("Downloading/loading tokenizer for: {}", MODEL_NAME);
            this.tokenizer = HuggingFaceTokenizer.newInstance(MODEL_NAME);
            log.info("Tokenizer loaded successfully from Hugging Face Hub");
        } catch (Exception e) {
            log.error("Failed to load tokenizer from Hugging Face", e);
            throw e;
        }
    }

    public long[] encode(String text) throws OrtException {
        if (tokenizer == null) throw new OrtException("Tokenizer not initialized");
        return tokenizer.encode(text.trim()).getIds();
    }

    public long[] getAttentionMask(String text) throws OrtException {
        if (tokenizer == null) return new long[0];
        return tokenizer.encode(text.trim()).getAttentionMask();
    }

    public long[] getTokenTypeIds(String text) throws OrtException {
        long[] ids = encode(text);
        return new long[ids.length]; // all 0s for single sentence
    }
}