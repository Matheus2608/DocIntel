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
     * Enable or disable Docling processing.
     * Default: true
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Client-side timeout for Docling API calls.
     * Must be >= server timeout to avoid premature timeouts on large documents.
     * Default: 3600s (1 hour)
     */
    @WithDefault("PT3600S")
    Duration clientTimeout();

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
