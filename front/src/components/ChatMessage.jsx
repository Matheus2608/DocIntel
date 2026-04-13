import { useEffect, useRef, useState } from 'react';
import { Bot, User, EyeClosed, ListChecks } from 'lucide-react';
import { getDarkModeClasses } from '../config';
import RagModal from './RagModal';

const TOOL_LABELS = {
    searchByHypotheticalQuestions: 'Busca por questões hipotéticas',
    searchByFakeAnswer: 'Busca por resposta hipotética (HyDE)',
    searchByKeyword: 'Busca por palavras-chave',
    getRecentUserMessages: 'Histórico: mensagens do usuário',
    getRecentAiResponses: 'Histórico: respostas do assistente',
};

const StepIcon = ({ status }) => {
    if (status === 'running') return <span className="animate-spin inline-block">⟳</span>;
    if (status === 'error') return <span className="text-red-500">✕</span>;
    return <span className="text-green-500">✓</span>;
};

const formatArgValue = (value) => {
    if (value === null || value === undefined) return '';
    if (Array.isArray(value)) return value.join(', ');
    if (typeof value === 'object') return JSON.stringify(value);
    if (typeof value === 'number') return Number.isInteger(value) ? String(value) : value.toFixed(2);
    return String(value);
};

const StepArguments = ({ args }) => {
    const parsed = typeof args === 'string' ? (() => { try { return JSON.parse(args); } catch { return null; } })() : args;
    if (!parsed || typeof parsed !== 'object') return null;
    const entries = Object.entries(parsed);
    if (entries.length === 0) return null;
    return (
        <ul className="mt-0.5 ml-5 pl-2 border-l border-gray-200 text-[11px] text-gray-500 space-y-0.5">
            {entries.map(([k, v]) => (
                <li key={k} className="break-words">
                    <span className="font-medium text-gray-600">{k}:</span> {formatArgValue(v)}
                </li>
            ))}
        </ul>
    );
};

const AgentSteps = ({ steps, defaultExpanded = false }) => {
    const [expanded, setExpanded] = useState(defaultExpanded);

    return (
        <div className="text-xs text-gray-500 mt-1 mb-2">
            <button onClick={() => setExpanded(e => !e)} className="flex items-center gap-1 hover:text-gray-700">
                <span>{expanded ? '▾' : '▸'}</span>
                <span>Passos do agente ({steps.length})</span>
            </button>

            {expanded && (
                <ul className="mt-1 pl-4 space-y-1">
                    {steps.map((s, i) => (
                        <li
                            key={i}
                            className="animate-in fade-in slide-in-from-left-1 duration-300"
                        >
                            <div className="flex items-center gap-1">
                                <StepIcon status={s.status} />
                                <span>{TOOL_LABELS[s.tool] ?? s.tool}</span>
                            </div>
                            <StepArguments args={s.arguments} />
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

/**
 * Componente para exibir a lista de mensagens do chat
 * Responsabilidade: Apenas renderização das mensagens e auto-scroll
 */
export const ChatMessage = ({ messages, isTyping, liveStep, isDarkMode, fetchSteps }) => {
    const messagesEndRef = useRef(null);
    const [selectedChat, setSelectedChat] = useState(null)
    const [historicSteps, setHistoricSteps] = useState({}); // { [messageId]: steps[] }
    const onCloseSelectedMessage = () => setSelectedChat(null);

    const handleShowSteps = async (messageId) => {
        if (historicSteps[messageId]) {
            setHistoricSteps(prev => {
                const next = { ...prev };
                delete next[messageId];
                return next;
            });
            return;
        }
        if (!fetchSteps) return;
        const steps = await fetchSteps(messageId);
        setHistoricSteps(prev => ({ ...prev, [messageId]: steps }));
    };

    // Auto-scroll para a última mensagem
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const { pageBgAndText } = getDarkModeClasses(isDarkMode);

    return (
        <div className={`flex-1 overflow-y-auto p-4 space-y-4 ${pageBgAndText}`}>
            {messages.length === 0 && (
                <div className="text-center text-gray-500 mt-8">
                    <Bot className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                    <p className="text-lg font-medium">Olá! Como posso ajudá-lo?</p>
                    <p className="text-sm">Faça uma pergunta sobre seu documento</p>
                </div>
            )}

            {messages.map((m, idx) =>
                <div key={m.id ? m.id : idx} className="flex flex-col gap-1">
                    <div
                        className={`flex gap-3 ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}
                    >
                        {m.role === 'assistant' && (
                            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center">
                                <Bot className="w-5 h-5 text-white" />
                            </div>
                        )}

                        <div
                            className={`max-w-[75%] rounded-2xl px-4 py-3 ${m.role === 'user'
                                ? 'bg-blue-600 text-white rounded-tr-sm'
                                : 'bg-white text-gray-800 shadow-sm rounded-tl-sm border border-gray-200'
                                }`}
                        >
                            {m.role === 'assistant' && m.steps?.length > 0 && <AgentSteps steps={m.steps} />}
                            <p className="text-sm whitespace-pre-wrap break-words">{m.text}</p>
                        </div>

                        {m.role === 'user' && (
                            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-gray-600 flex items-center justify-center">
                                <User className="w-5 h-5 text-white" />
                            </div>
                        )}
                    </div>
                    {selectedChat === m.id ? (
                        <RagModal onClose={onCloseSelectedMessage} messageId={m.id} />
                    ) : (
                        m.id && m.role === "user" && (
                            <div className={`flex gap-2 ${m.role === 'user' ? 'justify-end pr-11' : 'justify-start pl-11'}`}>
                                <EyeClosed
                                    onClick={() => setSelectedChat(m.id)}
                                    className="w-4 h-4 text-gray-400 cursor-pointer hover:text-gray-600"
                                />
                                {m.stepCount > 0 && (
                                    <ListChecks
                                        onClick={() => handleShowSteps(m.id)}
                                        className="w-4 h-4 text-gray-400 cursor-pointer hover:text-gray-600"
                                    />
                                )}
                            </div>
                        )
                    )}
                    {historicSteps[m.id] && historicSteps[m.id].length > 0 && (
                        <div className={`flex ${m.role === 'user' ? 'justify-end pr-11' : 'justify-start pl-11'}`}>
                            <div className="max-w-[75%] bg-gray-50 rounded-lg px-3 py-2 border border-gray-200">
                                <AgentSteps steps={historicSteps[m.id]} defaultExpanded />
                            </div>
                        </div>
                    )}
                </div>
            )
            }

            {
                isTyping && (
                    <div className="flex gap-3 justify-start">
                        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center">
                            <Bot className="w-5 h-5 text-white" />
                        </div>
                        <div className="bg-white rounded-2xl rounded-tl-sm px-4 py-3 shadow-sm border border-gray-200 max-w-[75%]">
                            {liveStep ? (
                                <div
                                    key={`${liveStep.tool}-${liveStep.startedAt ?? Date.now()}`}
                                    className="text-xs text-gray-600 animate-in fade-in slide-in-from-left-1 duration-300"
                                >
                                    <div className="flex items-center gap-1">
                                        <span className="animate-spin inline-block text-blue-500">⟳</span>
                                        <span className="font-medium">{TOOL_LABELS[liveStep.tool] ?? liveStep.tool}</span>
                                    </div>
                                    <StepArguments args={liveStep.arguments} />
                                </div>
                            ) : (
                                <div className="flex gap-1">
                                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
                                </div>
                            )}
                        </div>
                    </div>
                )
            }

            <div ref={messagesEndRef} />
        </div >
    );
};
