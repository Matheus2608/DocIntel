import { useState, useRef } from 'react';
import { UploadCloud, Loader2 } from 'lucide-react';
import { API_BASE_URL } from '../config';

export const Upload = ({ onUploadSuccess }) => {
    const [isDragging, setIsDragging] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [error, setError] = useState(null);
    const fileInputRef = useRef(null);

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

            // Chama o callback com os dados do chat
            if (onUploadSuccess) {
                onUploadSuccess(enrichedChatData);
            }
        } catch (err) {
            console.error('Erro no upload:', err);
            setError(err.message || 'Erro ao fazer upload. Tente novamente.');
        } finally {
            setIsUploading(false);
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
        fileInputRef.current?.click();
    };

    const handleFileSelect = (e) => {
        const file = e.target.files[0];
        handleFile(file);
    };

    return (
        <div className={`border-none flex-1 p-8 overflow-y-auto flex flex-col justify-center items-center border-none transition-colors`}>
            <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.doc,.docx"
                onChange={handleFileSelect}
                style={{ display: 'none' }}
            />
            
            <div 
                className={`w-full bg-white border-2 border-dashed rounded-2xl p-16 flex flex-col items-center justify-center transition-all cursor-pointer group ${
                    isDragging 
                        ? 'border-blue-600 bg-blue-100' 
                        : 'border-gray-300 hover:border-blue-400 hover:bg-blue-50'
                } ${isUploading ? 'pointer-events-none opacity-60' : ''}`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={!isUploading ? handleClick : undefined}
            >
                <div className={`p-4 rounded-full mb-4 group-hover:scale-110 transition-transform bg-blue-100`}>
                    {isUploading ? (
                        <Loader2 size={32} className="text-blue-600 animate-spin" />
                    ) : (
                        <UploadCloud size={32} className="text-blue-600" />
                    )}
                </div>
                <p className={`text-lg font-medium text-gray-700`}>
                    {isUploading ? 'Fazendo upload...' : 'Click to upload PDF or DOC'}
                </p>
                <p className={`text-sm mt-1 text-gray-400`}>
                    {isUploading ? 'Aguarde enquanto processamos seu arquivo' : 'ou arraste o arquivo aqui'}
                </p>
                
                {error && (
                    <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                        {error}
                    </div>
                )}
            </div>
        </div>
    )
}