import { useState } from 'react';
import { ChevronDown, ChevronUp, FileText, Percent, Hash, Clock, Edit2, Database } from 'lucide-react';
import type { EmbeddingEntry } from '../types/embedding';

interface EmbeddingSearchResultsProps {
  results: EmbeddingEntry[];
  loading?: boolean;
  onEdit?: (entry: EmbeddingEntry) => void;
  searchQuery?: string;
}

/**
 * Results display component for embedding search
 * Shows expandable cards with similarity scores and metadata
 */
export const EmbeddingSearchResults = ({ 
  results, 
  loading = false,
  onEdit,
  searchQuery 
}: EmbeddingSearchResultsProps) => {
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  const toggleExpanded = (id: string) => {
    setExpandedIds((prev: Set<string>) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const getSimilarityColor = (similarity: number): string => {
    if (similarity >= 0.9) return 'text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/20';
    if (similarity >= 0.8) return 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/20';
    if (similarity >= 0.7) return 'text-yellow-600 dark:text-yellow-400 bg-yellow-50 dark:bg-yellow-900/20';
    return 'text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-900/20';
  };

  const formatDate = (dateStr?: string): string => {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleString();
    } catch {
      return dateStr;
    }
  };

  // Loading state
  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <div className="w-12 h-12 border-4 border-blue-200 dark:border-blue-800 border-t-blue-600 dark:border-t-blue-400 rounded-full animate-spin mb-4" />
        <p className="text-gray-500 dark:text-gray-400">Searching embeddings...</p>
      </div>
    );
  }

  // Empty state (no search yet)
  if (!searchQuery && results.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 px-4">
        <Database className="w-16 h-16 text-gray-300 dark:text-gray-600 mb-4" />
        <h3 className="text-lg font-medium text-gray-700 dark:text-gray-300 mb-2">
          Search the Embedding Store
        </h3>
        <p className="text-gray-500 dark:text-gray-400 text-center max-w-md">
          Enter a text query above to search for similar content in the vector database.
          Results will be ranked by semantic similarity.
        </p>
      </div>
    );
  }

  // No results state
  if (searchQuery && results.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 px-4">
        <div className="bg-yellow-50 dark:bg-yellow-900/20 border-2 border-yellow-400 dark:border-yellow-600 rounded-xl p-8 max-w-md text-center">
          <FileText className="w-12 h-12 text-yellow-600 dark:text-yellow-400 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-yellow-800 dark:text-yellow-200 mb-2">
            No Results Found
          </h3>
          <p className="text-sm text-yellow-700 dark:text-yellow-300">
            No embeddings matched your query with the current similarity threshold.
            Try adjusting the minimum similarity or using different search terms.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Results header */}
      <div className="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
        <span>Found {results.length} result{results.length !== 1 ? 's' : ''}</span>
        {searchQuery && (
          <span className="italic">for "{searchQuery.length > 50 ? searchQuery.slice(0, 50) + '...' : searchQuery}"</span>
        )}
      </div>

      {/* Results list */}
      <div className="space-y-3">
        {results.map((entry, index) => {
          const isExpanded = expandedIds.has(entry.id);
          const similarity = entry.similarity ?? 0;
          
          return (
            <div 
              key={entry.id} 
              className="bg-white dark:bg-zinc-800 border border-gray-200 dark:border-zinc-700 rounded-xl overflow-hidden shadow-sm hover:shadow-md transition-shadow"
            >
              {/* Card header - always visible */}
              <div 
                className="p-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-zinc-700/50 transition-colors"
                onClick={() => toggleExpanded(entry.id)}
              >
                <div className="flex items-start gap-4">
                  {/* Rank number */}
                  <div className="flex-shrink-0 w-8 h-8 rounded-full bg-gray-100 dark:bg-zinc-700 flex items-center justify-center">
                    <span className="text-sm font-medium text-gray-600 dark:text-gray-300">
                      {index + 1}
                    </span>
                  </div>

                  {/* Content preview */}
                  <div className="flex-1 min-w-0">
                    <p className={`text-gray-800 dark:text-gray-100 ${isExpanded ? '' : 'line-clamp-2'}`}>
                      {entry.text}
                    </p>
                  </div>

                  {/* Similarity badge & expand button */}
                  <div className="flex items-center gap-3 flex-shrink-0">
                    <div className={`px-3 py-1 rounded-full text-sm font-medium ${getSimilarityColor(similarity)}`}>
                      <Percent className="w-3 h-3 inline mr-1" />
                      {(similarity * 100).toFixed(1)}%
                    </div>
                    
                    {isExpanded ? (
                      <ChevronUp className="w-5 h-5 text-gray-400" />
                    ) : (
                      <ChevronDown className="w-5 h-5 text-gray-400" />
                    )}
                  </div>
                </div>
              </div>

              {/* Expanded content */}
              {isExpanded && (
                <div className="border-t border-gray-100 dark:border-zinc-700 p-4 bg-gray-50/50 dark:bg-zinc-800/50 space-y-4 animate-in slide-in-from-top-2 duration-200">
                  {/* Full text */}
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400 mb-2">
                      Full Text
                    </h4>
                    <p className="text-sm text-gray-700 dark:text-gray-300 bg-white dark:bg-zinc-800 p-3 rounded-lg border border-gray-100 dark:border-zinc-700 whitespace-pre-wrap">
                      {entry.text}
                    </p>
                  </div>

                  {/* Metadata grid */}
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400 mb-2">
                      Metadata
                    </h4>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                      {/* Entry ID */}
                      <div className="flex items-center gap-2 text-sm">
                        <Hash className="w-4 h-4 text-gray-400" />
                        <span className="text-gray-500 dark:text-gray-400">ID:</span>
                        <span className="text-gray-700 dark:text-gray-300 font-mono text-xs truncate">
                          {entry.id}
                        </span>
                      </div>

                      {/* File name */}
                      {entry.metadata?.FILE_NAME && (
                        <div className="flex items-center gap-2 text-sm">
                          <FileText className="w-4 h-4 text-gray-400" />
                          <span className="text-gray-500 dark:text-gray-400">Source:</span>
                          <span className="text-gray-700 dark:text-gray-300 truncate">
                            {entry.metadata.FILE_NAME}
                          </span>
                        </div>
                      )}

                      {/* Created at */}
                      {entry.metadata?.created_at && (
                        <div className="flex items-center gap-2 text-sm">
                          <Clock className="w-4 h-4 text-gray-400" />
                          <span className="text-gray-500 dark:text-gray-400">Created:</span>
                          <span className="text-gray-700 dark:text-gray-300">
                            {formatDate(entry.metadata.created_at)}
                          </span>
                        </div>
                      )}

                      {/* Other metadata */}
                      {Object.entries(entry.metadata || {})
                        .filter(([key]) => !['FILE_NAME', 'entry_id', 'source', 'created_at', 'updated_at'].includes(key))
                        .map(([key, value]) => (
                          <div key={key} className="flex items-center gap-2 text-sm">
                            <span className="text-gray-500 dark:text-gray-400">{key}:</span>
                            <span className="text-gray-700 dark:text-gray-300 truncate">{value}</span>
                          </div>
                        ))
                      }
                    </div>
                  </div>

                  {/* Actions */}
                  {onEdit && (
                    <div className="flex justify-end pt-2 border-t border-gray-100 dark:border-zinc-700">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onEdit(entry);
                        }}
                        className="flex items-center gap-2 px-4 py-2 text-sm text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-colors"
                      >
                        <Edit2 className="w-4 h-4" />
                        Edit Entry
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default EmbeddingSearchResults;
