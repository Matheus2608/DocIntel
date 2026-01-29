import { useState } from 'react';
import { EMBEDDINGS_API_URL } from '../config';
import type {
  EmbeddingSearchRequest,
  EmbeddingSearchResponse,
  EmbeddingAddRequest,
  EmbeddingAddResponse,
  EmbeddingUpdateRequest,
  EmbeddingUpdateResponse,
  EmbeddingEntry,
} from '../types/embedding';

/**
 * Custom hook for embedding search and management operations
 */
export const useEmbeddingSearch = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [results, setResults] = useState<EmbeddingEntry[]>([]);

  /**
   * Search embeddings by text query
   */
  const search = async (request: EmbeddingSearchRequest): Promise<EmbeddingSearchResponse> => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${EMBEDDINGS_API_URL}/search`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`Search failed: ${response.statusText}`);
      }

      const data: EmbeddingSearchResponse = await response.json();
      setResults(data.results);
      return data;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Search failed';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  /**
   * Add a new embedding entry
   */
  const addEntry = async (request: EmbeddingAddRequest): Promise<EmbeddingAddResponse> => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(EMBEDDINGS_API_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`Add entry failed: ${response.statusText}`);
      }

      const data: EmbeddingAddResponse = await response.json();
      return data;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Add entry failed';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  /**
   * Get a single embedding entry by ID
   */
  const getEntry = async (entryId: string): Promise<EmbeddingEntry> => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${EMBEDDINGS_API_URL}/${entryId}`);

      if (!response.ok) {
        throw new Error(`Get entry failed: ${response.statusText}`);
      }

      const data: EmbeddingEntry = await response.json();
      return data;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Get entry failed';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  /**
   * Update an existing embedding entry
   */
  const updateEntry = async (request: EmbeddingUpdateRequest): Promise<EmbeddingUpdateResponse> => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${EMBEDDINGS_API_URL}/${request.entryId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`Update entry failed: ${response.statusText}`);
      }

      const data: EmbeddingUpdateResponse = await response.json();
      return data;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Update entry failed';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return {
    loading,
    error,
    results,
    search,
    addEntry,
    getEntry,
    updateEntry,
  };
};
