import { X, HelpCircle, Quote, AlertTriangle } from 'lucide-react';
import { useEffect, useState } from 'react';

const RagModal = ({ onClose, messageId }) => {
    const [questions, setQuestions] = useState([]);
    const [userQuestion, setUserQuestion] = useState(null);

    const response = {
        "id": "c8a5b15c-a359-4052-a6d9-24e9027c1b8f",
        "userQuestion": "A capital de Berlim não existe, pois Berlim é, na verdade, a capital da Alemanha.",
        "hypoteticalQuestions": [
            {
                "id": 8,
                "question": "Qual é a população de Berlim dentro dos limites da cidade?",
                "chunk": "Berlim (em alemão: Berlin, AFI: [bɛʁˈliːn], ouvirⓘ) é a capital e um dos dezesseis estados da Alemanha. Com uma população de 3,5 milhões dentro de limites da cidade, é a maior cidade do país, e a sétima área urbana mais povoada da União Europeia.[4] Situada no nordeste da Alemanha, é o centro da área metropolitana de Berlim-Brandemburgo, que inclui 5 milhões de pessoas de mais de 190 nações.[5] Localizada na grande planície europeia, Berlim é influenciada por um clima temperado sazonal. Cerca de um terço da área da cidade é composta por florestas, parques, jardins, rios e lagos.[6]\n\nDocumentada pela primeira vez no século XIII, Berlim foi sucessivamente a capital do Reino da Prússia (1701–1918), do Império Alemão (1871–1918), da República de Weimar (1919–1933) e do Terceiro Reich (1933–1945).[7] Após a Segunda Guerra Mundial, a cidade foi dividida; Berlim Oriental se tornou a capital da Alemanha Oriental, enquanto Berlim Ocidental se tornou um exclave da Alemanha Ocidental, cercada pelo muro de Berlim, entre os anos de 1961–1989; a cidade de Bona tornou-se a capital da Alemanha Ocidental.[8] Após a reunificação alemã em 1990, a cidade recuperou o seu estatuto como a capital da República Federal da Alemanha, sediando 147 embaixadas estrangeiras.[9][10]",
                "similarityScore": "0.7327743311389886"
            },
            {
                "id": 9,
                "question": "Qual foi a capital da Alemanha Ocidental após a divisão de Berlim após a Segunda Guerra Mundial?",
                "chunk": "Berlim (em alemão: Berlin, AFI: [bɛʁˈliːn], ouvirⓘ) é a capital e um dos dezesseis estados da Alemanha. Com uma população de 3,5 milhões dentro de limites da cidade, é a maior cidade do país, e a sétima área urbana mais povoada da União Europeia.[4] Situada no nordeste da Alemanha, é o centro da área metropolitana de Berlim-Brandemburgo, que inclui 5 milhões de pessoas de mais de 190 nações.[5] Localizada na grande planície europeia, Berlim é influenciada por um clima temperado sazonal. Cerca de um terço da área da cidade é composta por florestas, parques, jardins, rios e lagos.[6]\n\nDocumentada pela primeira vez no século XIII, Berlim foi sucessivamente a capital do Reino da Prússia (1701–1918), do Império Alemão (1871–1918), da República de Weimar (1919–1933) e do Terceiro Reich (1933–1945).[7] Após a Segunda Guerra Mundial, a cidade foi dividida; Berlim Oriental se tornou a capital da Alemanha Oriental, enquanto Berlim Ocidental se tornou um exclave da Alemanha Ocidental, cercada pelo muro de Berlim, entre os anos de 1961–1989; a cidade de Bona tornou-se a capital da Alemanha Ocidental.[8] Após a reunificação alemã em 1990, a cidade recuperou o seu estatuto como a capital da República Federal da Alemanha, sediando 147 embaixadas estrangeiras.[9][10]",
                "similarityScore": "0.7248891562091299"
            }
        ]
    }

    const retrievalInfo = async (messageId) => {
        const response = await fetch(`http://localhost:8080/api/retrieve/${messageId}`);
        const json = await response.json();
        return json;
    }

    useEffect(() => {
        const fetchData = async () => {
            const data = await retrievalInfo(messageId);
            console.log("retrievalInfo = ", data);
            setQuestions(data?.hypoteticalQuestions || []);
            setUserQuestion(data?.userQuestion || null);
        };
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
                    {questions.length === 0&& (
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
                        {questions.map((source, index) => (
                            <div key={source.id || index} className="bg-white dark:bg-[#24243e] border border-gray-200 dark:border-zinc-800 rounded-xl shadow-sm overflow-hidden">

                                {/* Parte Superior: A Conexão (Pergunta Hipotética) */}
                                <div className="p-4 bg-indigo-50 dark:bg-indigo-900/20 border-b border-indigo-100 dark:border-indigo-900/30 flex gap-3 items-start">
                                    <HelpCircle size={18} className="text-indigo-600 dark:text-indigo-400 mt-0.5 shrink-0" />
                                    <div>
                                        <span className="text-xs font-bold uppercase tracking-wider text-indigo-700 dark:text-indigo-300 block mb-1">Matched Question (HyDE)</span>
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
                        ))}
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