/**
 * TypeScript types for Embedding API
 */

export interface EmbeddingSearchRequest {
  query: string;
  maxResults?: number;
  minSimilarity?: number;
}

export interface EmbeddingEntry {
  id: string;
  text: string;
  similarity: number;
  metadata: Record<string, string>;
}

export interface EmbeddingSearchResponse {
  results: EmbeddingEntry[];
  totalResults: number;
}

export interface EmbeddingAddRequest {
  text: string;
  fileName?: string;
  customMetadata?: Record<string, string>;
}

export interface EmbeddingAddResponse {
  entryId: string;
  message: string;
}

export interface EmbeddingUpdateRequest {
  entryId: string;
  text?: string;
  fileName?: string;
  customMetadata?: Record<string, string>;
}

export interface EmbeddingUpdateResponse {
  entryId: string;
  message: string;
  reEmbedded: boolean;
}

export interface ErrorResponse {
  message: string;
}
