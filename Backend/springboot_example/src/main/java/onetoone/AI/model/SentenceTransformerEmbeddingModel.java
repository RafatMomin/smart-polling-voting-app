// src/main/java/onetoone/AI/model/SentenceTransformerEmbeddingModel.java
package onetoone.AI.model;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import jakarta.annotation.PostConstruct;
import onetoone.AI.util.HuggingFaceTokenizerClass;  // ← CORRECT
import ai.onnxruntime.*;
import onetoone.AI.util.HuggingFaceTokenizerClass;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;   // ADD THIS

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Component
public class SentenceTransformerEmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(SentenceTransformerEmbeddingModel.class);  // ADD THIS
    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizerClass tokenizer;

    private static final String MODEL_PATH = "models/sentence-transformers/all-MiniLM-L6-v2/model.onnx";
    private static final String TOKENIZER_PATH = "models/sentence-transformers/all-MiniLM-L6-v2/tokenizer.json";


    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment();
            log.info("ONNX Environment created");

            ClassPathResource modelRes = new ClassPathResource(MODEL_PATH);
            if (!modelRes.exists()) {
                throw new IllegalStateException("model.onnx not found: " + MODEL_PATH);
            }

            byte[] modelBytes = Files.readAllBytes(Paths.get(modelRes.getURI()));
            session = env.createSession(modelBytes, new OrtSession.SessionOptions());
            log.info("ONNX Session created");

            // DJL handles tokenizer download
            tokenizer = new HuggingFaceTokenizerClass();
            log.info("HuggingFace tokenizer initialized");

        } catch (Exception e) {
            log.error("Failed to initialize embedding model", e);
            throw new RuntimeException("Embedding model init failed", e);
        }
    }

    public float[] embed(String text) throws OrtException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Input text cannot be null or empty");
        }
        text = text.trim();

        long[] inputIds = tokenizer.encode(text);
        if (inputIds == null || inputIds.length == 0) {
            throw new OrtException("Tokenizer returned no tokens");
        }

        long[] attentionMask = tokenizer.getAttentionMask(text);
        long[] tokenTypeIds = tokenizer.getTokenTypeIds(text);

        try (OnnxTensor ids = OnnxTensor.createTensor(env, new long[][]{inputIds});
             OnnxTensor mask = OnnxTensor.createTensor(env, new long[][]{attentionMask});
             OnnxTensor types = OnnxTensor.createTensor(env, new long[][]{tokenTypeIds});
             OrtSession.Result result = session.run(Map.of(
                     "input_ids", ids,
                     "attention_mask", mask,
                     "token_type_ids", types
             ))) {

            Iterator<Map.Entry<String, OnnxValue>> it = result.iterator();
            if (!it.hasNext()) throw new OrtException("No output");

            OnnxValue value = it.next().getValue();
            if (!(value instanceof OnnxTensor tensor)) {
                throw new OrtException("Expected tensor");
            }

            Object raw = tensor.getValue();
            if (!(raw instanceof float[][][] embeddings)) {
                throw new OrtException("Expected float[][][], got: " + raw);
            }

            // CRITICAL: NULL CHECK HERE
            if (embeddings.length == 0) {
                throw new OrtException("Batch size is 0");
            }
            if (embeddings[0] == null) {
                log.error("embeddings[0] is NULL! Input: '{}', inputIds: {}", text, Arrays.toString(inputIds));
                throw new OrtException("Model returned null token embeddings");
            }
            if (embeddings[0].length == 0) {
                throw new OrtException("No tokens in sequence");
            }

            return normalize(meanPool(embeddings[0]));
        }
    }

    // meanPool()
    private float[] meanPool(float[][] tokenEmbeddings) {
        if (tokenEmbeddings == null || tokenEmbeddings.length == 0) {
            log.warn("meanPool: tokenEmbeddings is null or empty");
            return new float[384];
        }

        int dim = -1;
        float[] pooled = new float[384];
        int validCount = 0;

        for (int i = 0; i < tokenEmbeddings.length; i++) {
            float[] emb = tokenEmbeddings[i];
            if (emb == null) {
                log.warn("meanPool: tokenEmbeddings[{}] is null", i);
                continue;
            }
            if (emb.length == 0) {
                log.warn("meanPool: tokenEmbeddings[{}] is empty", i);
                continue;
            }

            if (dim == -1) {
                dim = emb.length;
            } else if (emb.length != dim) {
                log.warn("meanPool: dim mismatch: {} vs {}", emb.length, dim);
                continue;
            }

            boolean hasValue = false;
            for (float v : emb) {
                if (Math.abs(v) > 1e-8) {
                    hasValue = true;
                    break;
                }
            }
            if (!hasValue) continue;

            for (int j = 0; j < dim; j++) {
                pooled[j] += emb[j];
            }
            validCount++;
        }

        if (validCount == 0) {
            log.warn("meanPool: no valid embeddings, returning zero vector");
            return new float[dim >= 0 ? dim : 384];
        }

        for (int j = 0; j < dim; j++) {
            pooled[j] /= validCount;
        }
        return Arrays.copyOf(pooled, dim);
    }

    private float[] normalize(float[] v) {
        if (v == null || v.length == 0) return new float[384];
        float sum = 0;
        for (float x : v) sum += x * x;
        float norm = (float) Math.sqrt(sum);
        if (norm < 1e-8) return v;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i] / norm;
        return out;
    }

    public void close() throws OrtException {
        if (session != null) session.close();
        if (env != null) env.close();
    }

    // MAIN METHOD — RUN THIS!
    public static void main(String[] args) throws Exception {
        SentenceTransformerEmbeddingModel model = new SentenceTransformerEmbeddingModel();
        model.init();

        String[] tests = {"Hello world", "How are you?", "This is a test sentence."};
        for (String text : tests) {
            float[] vec = model.embed(text);
            System.out.printf("Text: \"%s\" → dim: %d, first 5: %s%n",
                    text, vec.length, Arrays.toString(Arrays.copyOf(vec, 5)));
        }

        model.close();
    }
}