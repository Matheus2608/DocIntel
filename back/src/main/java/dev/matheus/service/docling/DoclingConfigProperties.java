package dev.matheus.service.docling;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;

/**
 * Configuration properties for Docling document processing.
 * Mapped to application.properties under 'docling' prefix.
 */
@ConfigMapping(prefix = "docling")
public interface DoclingConfigProperties {


    /**
     * Chunking strategy to use.
     * Options: HIERARCHICAL, HYBRID
     * Default: HYBRID
     */
    ChunkingConfig chunking();

    /**
     * Chunking-specific configuration.
     */
    interface ChunkingConfig {

        /**
         * Chunking strategy: hierarchical or hybrid.
         * Default: hybrid
         */
        @WithDefault("hybrid")
        String strategy();

        /**
         * Maximum tokens per chunk.
         * Default: 2000
         */
        @WithDefault("2000")
        int maxTokens();
    }
}
