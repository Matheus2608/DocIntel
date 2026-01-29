import { useState } from 'react';
import { Plus, Database, RefreshCw } from 'lucide-react';
import EmbeddingSearchForm from './EmbeddingSearchForm';
import EmbeddingSearchResults from './EmbeddingSearchResults';
import EmbeddingAddModal from './EmbeddingAddModal';
import EmbeddingEditModal from './EmbeddingEditModal';
import { useEmbeddingSearch } from '../hooks/useEmbeddingSearch';
import type { EmbeddingEntry } from '../types/embedding';

/**
 * Main Embedding Search page component
 * Provides search, add, and edit functionality for embedding entries
 */
export const EmbeddingSearch = () => {
  const { loading, error, results, search, addEntry, updateEntry } = useEmbeddingSearch();
  
  // Modal states
  const [showAddModal, setShowAddModal] = useState(false);
  const [editEntry, setEditEntry] = useState<EmbeddingEntry | null>(null);
  
  // Search state for display
  const [lastQuery, setLastQuery] = useState<string>('');
  const [lastSearchParams, setLastSearchParams] = useState<{
    query: string;
    maxResults: number;
    minSimilarity: number;
  } | null>(null);

  const handleSearch = async (query: string, maxResults: number, minSimilarity: number) => {
    try {
      await search({
        query,
        maxResults,
        minSimilarity,
      });
      setLastQuery(query);
      setLastSearchParams({ query, maxResults, minSimilarity });
    } catch (err) {
      // Error is handled by the hook
      console.error('Search failed:', err);
    }
  };

  const handleRefresh = () => {
    if (lastSearchParams) {
      handleSearch(
        lastSearchParams.query,
        lastSearchParams.maxResults,
        lastSearchParams.minSimilarity
      );
    }
  };

  const handleAddEntry = async (text: string, fileName: string, customMetadata: Record<string, string>) => {
    await addEntry({
      text,
      fileName,
      customMetadata,
    });
    // Refresh results if we have a previous search
    if (lastSearchParams) {
      handleRefresh();
    }
  };

  const handleUpdateEntry = async (
    entryId: string, 
    text: string, 
    fileName: string, 
    customMetadata: Record<string, string>
  ) => {
    await updateEntry({
      entryId,
      text,
      fileName,
      customMetadata,
    });
    setEditEntry(null);
    // Refresh results
    if (lastSearchParams) {
      handleRefresh();
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-gray-50 dark:bg-zinc-900">
      {/* Header */}
      <div className="shrink-0 px-6 py-4 bg-white dark:bg-zinc-800 border-b border-gray-200 dark:border-zinc-700">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center shadow-lg">
              <Database className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-gray-800 dark:text-gray-100">
                Embedding Search
              </h1>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Search and manage vector embeddings
              </p>
            </div>
          </div>
          
          <div className="flex items-center gap-2">
            {lastSearchParams && (
              <button
                onClick={handleRefresh}
                disabled={loading}
                className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-700 rounded-lg transition-colors disabled:opacity-50"
                title="Refresh results"
              >
                <RefreshCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
              </button>
            )}
            <button
              onClick={() => setShowAddModal(true)}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-medium transition-colors shadow-sm"
            >
              <Plus className="w-5 h-5" />
              Add Entry
            </button>
          </div>
        </div>

        {/* Search Form */}
        <EmbeddingSearchForm
          onSearch={handleSearch}
          loading={loading}
        />
      </div>

      {/* Results Area */}
      <div className="flex-1 overflow-y-auto p-6">
        {/* Error message */}
        {error && (
          <div className="mb-4 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl text-red-700 dark:text-red-300">
            <p className="font-medium">Error</p>
            <p className="text-sm">{error}</p>
          </div>
        )}

        {/* Results */}
        <EmbeddingSearchResults
          results={results}
          loading={loading}
          searchQuery={lastQuery}
          onEdit={(entry) => setEditEntry(entry)}
        />
      </div>

      {/* Add Modal */}
      <EmbeddingAddModal
        isOpen={showAddModal}
        onClose={() => setShowAddModal(false)}
        onSubmit={handleAddEntry}
        loading={loading}
      />

      {/* Edit Modal */}
      <EmbeddingEditModal
        isOpen={!!editEntry}
        onClose={() => setEditEntry(null)}
        entry={editEntry}
        onSubmit={handleUpdateEntry}
        loading={loading}
      />
    </div>
  );
};

export default EmbeddingSearch;
