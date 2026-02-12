import { X, HelpCircle, Quote, AlertTriangle } from 'lucide-react';
import { useEffect, useState } from 'react';

const RagModal = ({ onClose, messageId }) => {
    const [questions, setQuestions] = useState([]);
    const [userQuestion, setUserQuestion] = useState(null);

    const retrievalInfo = async (messageId) => {
        const response = await fetch(`http://localhost:8080/api/retrieve/${messageId}`);

        if (!response.ok || response.status !== 200)
            return null;

        const json = await response.json();
        return json;
    }

    const sortBySimilarity = (a, b) => {
        return parseFloat(b.similarityScore) - parseFloat(a.similarityScore);
    }


    const fetchData = async () => {
        const data = await retrievalInfo(messageId);
        console.log("retrievalInfo = ", data);
        const sortedQuestions = (data?.hypoteticalQuestions || []).sort(sortBySimilarity);
        console.log("sortedQuestions = ", sortedQuestions);
        setQuestions(sortedQuestions);
        setUserQuestion(data?.userQuestion || null);
    };

    useEffect(() => {
        fetchData();
    }, [messageId]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#1a1a2e]/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
            <div className="bg-white dark:bg-[#24243e] w-full max-w-4xl rounded-2xl shadow-2xl overflow-hidden border border-gray-200 dark:border-zinc-700/50 flex flex-col max-h-[90vh]">

                {/* Header Fixo */}
                <div className="flex items-center justify-between p-6 border-b border-gray-100 dark:border-zinc-800 bg-white dark:bg-[#24243e] z-10 shrink-0">
                    <div>
                        <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100">
                            Fontes e Conexões do RAG
                        </h2>
                        {userQuestion && (
                            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                                Para a pergunta: <span className="text-indigo-600 dark:text-indigo-400 font-medium italic">"{userQuestion}"</span>
                            </p>
                        )}
                    </div>

                    <button
                        onClick={onClose}
                        className="p-2 bg-gray-100 hover:bg-gray-200 dark:bg-zinc-800 dark:hover:bg-zinc-700 rounded-full transition-colors"
                    >
                        <X size={20} className="text-gray-600 dark:text-gray-300" />
                    </button>
                </div>

                {/* Content Scrollável */}
                <div className="p-6 overflow-y-auto bg-gray-50/50 dark:bg-[#1a1a2e]/50 flex-grow">
                    {questions.length === 0 && (
                        <div className="flex flex-col items-center justify-center py-12 px-4">
                            <div className="bg-yellow-50 dark:bg-yellow-900/20 border-2 border-yellow-400 dark:border-yellow-600 rounded-xl p-8 max-w-md text-center">
                                <AlertTriangle size={48} className="text-yellow-600 dark:text-yellow-400 mx-auto mb-4" />
                                <h3 className="text-lg font-semibold text-yellow-800 dark:text-yellow-200 mb-2">
                                    Nenhuma Fonte RAG Encontrada
                                </h3>
                                <p className="text-sm text-yellow-700 dark:text-yellow-300">
                                    Não existem informações do RAG disponíveis para esta pergunta.
                                </p>
                            </div>
                        </div>
                    )}

                    <div className="space-y-6">
                        {questions.map((source, index) => {
                            const isHyDeMatch = source.question !== userQuestion;
                            const headerColor = isHyDeMatch
                                ? "bg-indigo-50 dark:bg-indigo-900/20 border-indigo-100 dark:border-indigo-900/30"
                                : "bg-emerald-50 dark:bg-emerald-900/20 border-emerald-100 dark:border-emerald-900/30";
                            const iconColor = isHyDeMatch
                                ? "text-indigo-600 dark:text-indigo-400"
                                : "text-emerald-600 dark:text-emerald-400";
                            const labelColor = isHyDeMatch
                                ? "text-indigo-700 dark:text-indigo-300"
                                : "text-emerald-700 dark:text-emerald-300";
                            const headerLabel = isHyDeMatch
                                ? "Matched Question (HyDE)"
                                : "Direct Content Match";

                            return (
                                <div key={source.id || index} className="bg-white dark:bg-[#24243e] border border-gray-200 dark:border-zinc-800 rounded-xl shadow-sm overflow-hidden">

                                    {/* Parte Superior: A Conexão (Pergunta Hipotética ou Match Direto) */}
                                    <div className={`p-4 ${headerColor} border-b flex gap-3 items-start`}>
                                        <HelpCircle size={18} className={`${iconColor} mt-0.5 shrink-0`} />
                                        <div>
                                            <span className={`text-xs font-bold uppercase tracking-wider ${labelColor} block mb-1`}>{headerLabel}</span>
                                            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                                                "{source.question}"
                                            </p>
                                        </div>
                                    </div>

                                {/* Parte Inferior: A Evidência e o Score */}
                                <div className="p-4 flex flex-col md:flex-row gap-6">
                                    {/* Trecho do Documento */}
                                    <div className="flex-1 flex gap-3">
                                        <Quote size={18} className="text-gray-400 shrink-0 mt-1" />
                                        <div>
                                            <span className="text-xs font-bold uppercase tracking-wider text-gray-500 block mb-1">Trecho Recuperado</span>
                                            <p className="text-sm text-gray-600 dark:text-gray-300 leading-relaxed italic bg-gray-50 dark:bg-zinc-800/50 p-3 rounded-lg border border-gray-100 dark:border-zinc-800">
                                                "{source.chunk}"
                                            </p>
                                        </div>
                                    </div>

                                    {/* Score de Similaridade Visual */}
                                    <div className="md:w-48 shrink-0 flex flex-col justify-center border-t md:border-t-0 md:border-l border-gray-100 dark:border-zinc-800 pt-4 md:pt-0 md:pl-6">
                                        <span className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-2 block text-center md:text-left">
                                            Similaridade: <span className="font-bold text-indigo-600 dark:text-indigo-400">{(parseFloat(source.similarityScore) * 100).toFixed(0)}%</span>
                                        </span>
                                        <div className="flex gap-1 h-6 justify-center md:justify-start">
                                            {[...Array(10)].map((_, i) => (
                                                <div
                                                    key={i}
                                                    className={`flex-1 rounded-sm transition-all duration-500 ${i < (parseFloat(source.similarityScore) * 10)
                                                        ? 'bg-indigo-500 dark:bg-indigo-500 shadow-sm'
                                                        : 'bg-gray-200 dark:bg-zinc-700 opacity-50'
                                                        }`}
                                                />
                                            ))}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        );
                        })}
                    </div>
                </div>

                {/* Footer Fixo */}
                <div className="p-4 border-t border-gray-100 dark:border-zinc-800 bg-white dark:bg-[#24243e] text-right shrink-0">
                    <button
                        onClick={onClose}
                        className="text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors px-4 py-2"
                    >
                        Fechar
                    </button>
                </div>
            </div>
        </div >
    );
};

export default RagModal;