import { useState, FormEvent, useEffect } from 'react';
import { X, Plus, Loader2, FileText } from 'lucide-react';

interface EmbeddingAddModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (text: string, fileName: string, customMetadata: Record<string, string>) => Promise<void>;
  loading?: boolean;
}

/**
 * Modal for adding new embedding entries
 * Follows RagModal design patterns
 */
export const EmbeddingAddModal = ({ 
  isOpen, 
  onClose, 
  onSubmit,
  loading = false 
}: EmbeddingAddModalProps) => {
  const [text, setText] = useState('');
  const [fileName, setFileName] = useState('');
  const [metadataKey, setMetadataKey] = useState('');
  const [metadataValue, setMetadataValue] = useState('');
  const [customMetadata, setCustomMetadata] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);

  // Reset form when modal opens
  useEffect(() => {
    if (isOpen) {
      setText('');
      setFileName('');
      setMetadataKey('');
      setMetadataValue('');
      setCustomMetadata({});
      setError(null);
    }
  }, [isOpen]);

  // Handle escape key
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose]);

  const handleAddMetadata = () => {
    if (metadataKey.trim() && metadataValue.trim()) {
      setCustomMetadata((prev: Record<string, string>) => ({
        ...prev,
        [metadataKey.trim()]: metadataValue.trim()
      }));
      setMetadataKey('');
      setMetadataValue('');
    }
  };

  const handleRemoveMetadata = (key: string) => {
    setCustomMetadata((prev: Record<string, string>) => {
      const next = { ...prev };
      delete next[key];
      return next;
    });
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!text.trim()) {
      setError('Text content is required');
      return;
    }

    try {
      await onSubmit(text.trim(), fileName.trim() || 'manual-entry', customMetadata);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add entry');
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-white dark:bg-zinc-800 w-full max-w-2xl rounded-2xl shadow-2xl overflow-hidden border border-gray-200 dark:border-zinc-700 flex flex-col max-h-[90vh]">
        
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-100 dark:border-zinc-700 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
              <Plus className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100">
                Add New Embedding
              </h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Add text content to the vector database
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 dark:hover:bg-zinc-700 rounded-full transition-colors"
          >
            <X className="w-5 h-5 text-gray-500 dark:text-gray-400" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Error message */}
          {error && (
            <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-700 dark:text-red-300 text-sm">
              {error}
            </div>
          )}

          {/* Text content */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Text Content <span className="text-red-500">*</span>
            </label>
            <textarea
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Enter the text content to embed..."
              rows={6}
              maxLength={10000}
              className="w-full px-4 py-3 bg-white dark:bg-zinc-900 border border-gray-200 dark:border-zinc-700 rounded-xl text-gray-800 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
            />
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 text-right">
              {text.length}/10,000 characters
            </p>
          </div>

          {/* File name / Source */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              <FileText className="w-4 h-4 inline mr-1" />
              Source Name
            </label>
            <input
              type="text"
              value={fileName}
              onChange={(e) => setFileName(e.target.value)}
              placeholder="e.g., manual-entry, notes, reference-doc"
              className="w-full px-4 py-3 bg-white dark:bg-zinc-900 border border-gray-200 dark:border-zinc-700 rounded-xl text-gray-800 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Optional identifier for filtering (defaults to "manual-entry")
            </p>
          </div>

          {/* Custom metadata */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Custom Metadata
            </label>
            
            {/* Add metadata input */}
            <div className="flex gap-2 mb-3">
              <input
                type="text"
                value={metadataKey}
                onChange={(e) => setMetadataKey(e.target.value)}
                placeholder="Key"
                className="flex-1 px-3 py-2 bg-white dark:bg-zinc-900 border border-gray-200 dark:border-zinc-700 rounded-lg text-gray-800 dark:text-gray-100 placeholder-gray-400 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <input
                type="text"
                value={metadataValue}
                onChange={(e) => setMetadataValue(e.target.value)}
                placeholder="Value"
                className="flex-1 px-3 py-2 bg-white dark:bg-zinc-900 border border-gray-200 dark:border-zinc-700 rounded-lg text-gray-800 dark:text-gray-100 placeholder-gray-400 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddMetadata();
                  }
                }}
              />
              <button
                type="button"
                onClick={handleAddMetadata}
                disabled={!metadataKey.trim() || !metadataValue.trim()}
                className="px-4 py-2 bg-gray-100 dark:bg-zinc-700 hover:bg-gray-200 dark:hover:bg-zinc-600 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg transition-colors"
              >
                <Plus className="w-4 h-4 text-gray-600 dark:text-gray-300" />
              </button>
            </div>

            {/* Metadata tags */}
            {Object.keys(customMetadata).length > 0 && (
              <div className="flex flex-wrap gap-2">
                {Object.entries(customMetadata).map(([key, value]) => (
                  <span
                    key={key}
                    className="inline-flex items-center gap-1 px-3 py-1 bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 rounded-full text-sm"
                  >
                    <span className="font-medium">{key}:</span> {String(value)}
                    <button
                      type="button"
                      onClick={() => handleRemoveMetadata(key)}
                      className="ml-1 hover:text-red-500 transition-colors"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>
        </form>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 p-6 border-t border-gray-100 dark:border-zinc-700 shrink-0">
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-zinc-700 rounded-lg transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={!text.trim() || loading}
            className="px-6 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 dark:disabled:bg-zinc-600 text-white rounded-lg font-medium transition-colors disabled:cursor-not-allowed flex items-center gap-2"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Adding...
              </>
            ) : (
              <>
                <Plus className="w-4 h-4" />
                Add Embedding
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default EmbeddingAddModal;
