import { useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload } from 'lucide-react'; // Ícone opcional

const DocumentQA = () => {
  const onDrop = useCallback((acceptedFiles) => {
    // Aqui você lida com o arquivo (ex: enviar para um backend ou processar)
    console.log(acceptedFiles);
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'application/msword': ['.doc'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx']
    }
  });

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100 p-4">
      <div className="w-full max-w-2xl bg-white rounded-lg shadow-xl overflow-hidden">
        
        {/* Header Azul */}
        <div className="bg-blue-600 p-6 text-white">
          <h2 className="text-2xl font-bold">Document Q&A</h2>
          <p className="text-blue-100">Upload a PDF or DOC and ask questions about its content</p>
        </div>

        {/* Área de Dropzone */}
        <div className="p-8">
          <div 
            {...getRootProps()} 
            className={`border-2 border-dashed rounded-xl p-12 flex flex-col items-center justify-center cursor-pointer transition-colors
              ${isDragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-blue-400'}`}
          >
            <input {...getInputProps()} />
            <Upload className="w-12 h-12 text-gray-400 mb-4" />
            <p className="text-gray-600 text-lg">
              {isDragActive ? "Solte o arquivo aqui..." : "Click to upload PDF or DOC"}
            </p>
          </div>

          {/* Input de Pergunta (Footer) */}
          <div className="mt-8 flex gap-2">
            <input 
              disabled
              placeholder="Upload a document first"
              className="flex-1 p-3 border border-gray-300 rounded-lg bg-gray-50 outline-none"
            />
            <button className="bg-blue-600 text-white px-6 py-2 rounded-lg flex items-center gap-2 hover:bg-blue-700 transition">
              <span>Ask</span>
              <svg className="w-4 h-4 rotate-90" fill="currentColor" viewBox="0 0 20 20"><path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z" /></svg>
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};

export default DocumentQA;