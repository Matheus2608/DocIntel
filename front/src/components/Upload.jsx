import { useState, useRef, useEffect } from 'react';
import { UploadCloud, Loader2, CheckCircle, AlertCircle } from 'lucide-react';
import { API_BASE_URL } from '../config';

export const Upload = ({ onUploadSuccess }) => {
    const [isDragging, setIsDragging] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);
    const [processingStatus, setProcessingStatus] = useState(null);
    const [error, setError] = useState(null);
    const [currentChatId, setCurrentChatId] = useState(null);
    const fileInputRef = useRef(null);
    const pollingIntervalRef = useRef(null);

    // Poll for document processing status
    useEffect(() => {
        if (currentChatId && isProcessing) {
            pollingIntervalRef.current = setInterval(async () => {
                try {
                    const response = await fetch(`${API_BASE_URL}/${currentChatId}/document/status`);
                    if (!response.ok) {
                        throw new Error('Failed to check processing status');
                    }
                    
                    const status = await response.json();
                    setProcessingStatus(status);
                    
                    // Check if processing is complete
                    if (status.status === 'COMPLETED') {
                        clearInterval(pollingIntervalRef.current);
                        setIsProcessing(false);
                        console.log('Document processing completed!', status);
                    } else if (status.status === 'FAILED') {
                        clearInterval(pollingIntervalRef.current);
                        setIsProcessing(false);
                        setError(`Processing failed: ${status.error || 'Unknown error'}`);
                    }
                } catch (err) {
                    console.error('Error polling status:', err);
                }
            }, 3000); // Poll every 3 seconds
            
            return () => {
                if (pollingIntervalRef.current) {
                    clearInterval(pollingIntervalRef.current);
                }
            };
        }
    }, [currentChatId, isProcessing]);

    const handleFile = async (file) => {
        if (!file) return;

        // Validar tipo de arquivo
        const validTypes = [
            'application/pdf',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/msword',
            'text/plain'
        ];

        console.log('Tipo do arquivo:', file.type);

        if (!validTypes.includes(file.type)) {
            setError('Tipo de arquivo inválido. Apenas PDF e DOCX são permitidos.');
            return;
        }

        setError(null);
        setIsUploading(true);
        setProcessingStatus(null);

        try {
            const formData = new FormData();
            formData.append('file', file);
            
            const title = file.name.replace(/\.[^/.]+$/, '');

            const response = await fetch(API_BASE_URL, {
                method: 'POST',
                body: formData,
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Erro ao fazer upload do arquivo');
            }

            const chatData = await response.json();

            const enrichedChatData = {
                ...chatData,
                fileName: file.name,
                title: title
            };

            // Start polling for processing status
            setCurrentChatId(chatData.id);
            setIsUploading(false);
            setIsProcessing(true);
            setProcessingStatus({ status: 'PROCESSING' });

            // Chama o callback com os dados do chat
            if (onUploadSuccess) {
                onUploadSuccess(enrichedChatData);
            }
        } catch (err) {
            console.error('Erro no upload:', err);
            setError(err.message || 'Erro ao fazer upload. Tente novamente.');
            setIsUploading(false);
            setIsProcessing(false);
        }
    };

    const handleDragOver = (e) => {
        e.preventDefault();
        setIsDragging(true);
    };

    const handleDragLeave = (e) => {
        e.preventDefault();
        setIsDragging(false);
    };

    const handleDrop = (e) => {
        e.preventDefault();
        setIsDragging(false);
        
        const file = e.dataTransfer.files[0];
        handleFile(file);
    };

    const handleClick = () => {
        if (!isUploading && !isProcessing) {
            fileInputRef.current?.click();
        }
    };

    const handleFileSelect = (e) => {
        const file = e.target.files[0];
        handleFile(file);
    };

    const getStatusMessage = () => {
        if (isUploading) return 'Uploading file...';
        if (isProcessing) {
            if (processingStatus?.status === 'PROCESSING') {
                return processingStatus.chunkCount 
                    ? `Processing document... (${processingStatus.chunkCount} chunks created)`
                    : 'Processing document with Docling AI...';
            }
            if (processingStatus?.status === 'PENDING') {
                return 'Queued for processing...';
            }
        }
        return 'Click to upload PDF or DOC';
    };

    const getStatusIcon = () => {
        if (isUploading || isProcessing) {
            return <Loader2 size={32} className="text-blue-600 animate-spin" />;
        }
        if (processingStatus?.status === 'COMPLETED') {
            return <CheckCircle size={32} className="text-green-600" />;
        }
        if (error || processingStatus?.status === 'FAILED') {
            return <AlertCircle size={32} className="text-red-600" />;
        }
        return <UploadCloud size={32} className="text-blue-600" />;
    };

    const isDisabled = isUploading || isProcessing;

    return (
        <div className={`border-none flex-1 p-8 overflow-y-auto flex flex-col justify-center items-center border-none transition-colors`}>
            <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.doc,.docx"
                onChange={handleFileSelect}
                style={{ display: 'none' }}
                disabled={isDisabled}
            />
            
            <div 
                className={`w-full bg-white border-2 border-dashed rounded-2xl p-16 flex flex-col items-center justify-center transition-all cursor-pointer group ${
                    isDragging 
                        ? 'border-blue-600 bg-blue-100' 
                        : 'border-gray-300 hover:border-blue-400 hover:bg-blue-50'
                } ${isDisabled ? 'pointer-events-none opacity-60' : ''}`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={handleClick}
            >
                <div className={`p-4 rounded-full mb-4 group-hover:scale-110 transition-transform bg-blue-100`}>
                    {getStatusIcon()}
                </div>
                <p className={`text-lg font-medium text-gray-700`}>
                    {getStatusMessage()}
                </p>
                <p className={`text-sm mt-1 text-gray-400`}>
                    {isUploading && 'Sending file to server...'}
                    {isProcessing && 'This may take a few minutes for large documents'}
                    {!isUploading && !isProcessing && 'ou arraste o arquivo aqui'}
                </p>
                
                {processingStatus?.status === 'COMPLETED' && (
                    <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">
                        ✓ Document processed successfully! {processingStatus.chunkCount} chunks created.
                    </div>
                )}
                
                {error && (
                    <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                        {error}
                    </div>
                )}
            </div>
        </div>
    )
}
