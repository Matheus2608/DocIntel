import { useState, FormEvent } from 'react';
import { Search, Loader2, SlidersHorizontal } from 'lucide-react';

interface EmbeddingSearchFormProps {
  onSearch: (query: string, maxResults: number, minSimilarity: number) => void;
  loading?: boolean;
  disabled?: boolean;
}

/**
 * Search form component for embedding search
 * Follows existing project patterns (ChatMessage, InputMessage)
 */
export const EmbeddingSearchForm = ({ 
  onSearch, 
  loading = false, 
  disabled = false 
}: EmbeddingSearchFormProps) => {
  const [query, setQuery] = useState('');
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [maxResults, setMaxResults] = useState(10);
  const [minSimilarity, setMinSimilarity] = useState(0.7);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (query.trim() && !loading && !disabled) {
      onSearch(query.trim(), maxResults, minSimilarity);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e as unknown as FormEvent);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* Main search input */}
      <div className="flex gap-2">
        <div className="relative flex-1">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Search embeddings by text..."
            disabled={disabled || loading}
            className="w-full px-4 py-3 pl-11 bg-white dark:bg-zinc-800 border border-gray-200 dark:border-zinc-700 rounded-xl text-gray-800 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          />
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
        </div>
        
        <button
          type="button"
          onClick={() => setShowAdvanced(!showAdvanced)}
          className={`p-3 rounded-xl border transition-colors ${
            showAdvanced 
              ? 'bg-blue-50 dark:bg-blue-900/30 border-blue-200 dark:border-blue-800 text-blue-600 dark:text-blue-400' 
              : 'bg-white dark:bg-zinc-800 border-gray-200 dark:border-zinc-700 text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
          }`}
          title="Advanced options"
        >
          <SlidersHorizontal className="w-5 h-5" />
        </button>

        <button
          type="submit"
          disabled={!query.trim() || loading || disabled}
          className="px-6 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 dark:disabled:bg-zinc-700 text-white rounded-xl font-medium transition-colors disabled:cursor-not-allowed flex items-center gap-2"
        >
          {loading ? (
            <>
              <Loader2 className="w-5 h-5 animate-spin" />
              Searching...
            </>
          ) : (
            <>
              <Search className="w-5 h-5" />
              Search
            </>
          )}
        </button>
      </div>

      {/* Advanced options */}
      {showAdvanced && (
        <div className="p-4 bg-gray-50 dark:bg-zinc-800/50 rounded-xl border border-gray-100 dark:border-zinc-700 space-y-4 animate-in slide-in-from-top-2 duration-200">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Max Results
              </label>
              <input
                type="number"
                value={maxResults}
                onChange={(e) => setMaxResults(Math.max(1, Math.min(100, parseInt(e.target.value) || 10)))}
                min={1}
                max={100}
                className="w-full px-3 py-2 bg-white dark:bg-zinc-800 border border-gray-200 dark:border-zinc-700 rounded-lg text-gray-800 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">1-100 results</p>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Min Similarity: {(minSimilarity * 100).toFixed(0)}%
              </label>
              <input
                type="range"
                value={minSimilarity}
                onChange={(e) => setMinSimilarity(parseFloat(e.target.value))}
                min={0}
                max={1}
                step={0.05}
                className="w-full h-2 bg-gray-200 dark:bg-zinc-700 rounded-lg appearance-none cursor-pointer accent-blue-600"
              />
              <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mt-1">
                <span>0%</span>
                <span>100%</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </form>
  );
};

export default EmbeddingSearchForm;
