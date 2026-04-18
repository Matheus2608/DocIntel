import { useState } from 'react';
import { Mail, Lock, User, FileText, CheckCircle2, ArrowRight } from 'lucide-react';

const AuthPage = () => {
  const [isLogin, setIsLogin] = useState(true);

  return (
    <div style={{
      width: '100vw',
      height: '100vh',
      position: 'relative',
      display: 'flex',
      backgroundColor: '#0f172a',
      color: '#cbd5e1',
      fontFamily: 'sans-serif'
    }}>
      {/* Background Image */}
      <img 
        src="/logo.png" 
        alt="background" 
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          zIndex: -10,
          opacity: 0.3
        }}
      />
      
      {/* Overlay escuro */}
      <div style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        backgroundColor: 'rgba(0, 0, 0, 0.4)',
        zIndex: -9
      }}></div>
      
      {/* PAINEL ESQUERDO: Branding & Visual (Oculto em Mobile) */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-[#111827] to-[#1e3a8a] flex-col justify-center items-center p-12 relative overflow-hidden border-r border-slate-800" style={{ zIndex: 10 }}>
        
        {/* Elemento Decorativo: Grid de Fundo */}
        <div className="absolute inset-0 opacity-10 pointer-events-none" 
             style={{ backgroundImage: 'radial-gradient(#3b82f6 1px, transparent 1px)', backgroundSize: '40px 40px' }}>
        </div>

        <div className="relative z-10 w-full max-w-md">
          <div className="flex items-center gap-3 mb-8">
            <div className="bg-blue-600 p-2 rounded-lg">
              <FileText size={32} className="text-white" />
            </div>
            <h1 className="text-4xl font-bold tracking-tight text-white">DocIntel</h1>
          </div>

          <h2 className="text-2xl font-semibold text-blue-400 mb-6">
            Inteligência documental para o futuro.
          </h2>

          <ul className="space-y-6">
            <li className="flex items-start gap-4">
              <CheckCircle2 className="text-blue-500 mt-1 flex-shrink-0" size={20} />
              <p className="text-slate-300">Respostas precisas baseadas nos seus próprios documentos usando RAG.</p>
            </li>
            <li className="flex items-start gap-4">
              <CheckCircle2 className="text-blue-500 mt-1 flex-shrink-0" size={20} />
              <p className="text-slate-300">Análise de dados complexos com agentes inteligentes em segundos.</p>
            </li>
            <li className="flex items-start gap-4">
              <CheckCircle2 className="text-blue-500 mt-1 flex-shrink-0" size={20} />
              <p className="text-slate-300">Infraestrutura segura e escalável para sua empresa.</p>
            </li>
          </ul>

          <div className="mt-12 p-6 bg-slate-900/50 backdrop-blur-sm border border-slate-700/50 rounded-2xl">
            <p className="italic text-slate-400 text-sm">
              "A ferramenta que faltava para transformar nossos PDFs em conhecimento acionável."
            </p>
            <p className="mt-4 text-sm font-medium text-slate-300">— Equipe de Engenharia DocIntel</p>
          </div>
        </div>
      </div>
      
      {/* PAINEL DIREITO: Formulários */}
      <div className="w-full lg:w-1/2 flex flex-col justify-center items-center p-8" style={{ zIndex: 10, backgroundColor: '#0f172a' }}>
        
        <div className="w-full max-w-md space-y-8">
          
          {/* Cabeçalho do Formulário */}
          <div className="text-center lg:text-left">
            <h2 className="text-3xl font-bold text-white">
              {isLogin ? 'Bem-vindo de volta' : 'Crie sua conta'}
            </h2>
            <p className="text-slate-400 mt-2">
              {isLogin 
                ? 'Acesse sua conta para continuar' 
                : 'Comece sua jornada no DocIntel agora mesmo'}
            </p>
          </div>

          <form className="mt-8 space-y-6" onSubmit={(e) => e.preventDefault()}>
            
            <div className="space-y-4">
              
              {/* Nome (Apenas no Registro) */}
              {!isLogin && (
                <div className="relative group">
                  <label className="text-sm font-medium text-slate-300 block mb-1">Nome Completo</label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-blue-500 transition-colors" size={18} />
                    <input 
                      type="text" 
                      placeholder="Ex: Matheus Silva"
                      className="w-full bg-slate-900 border border-slate-700 text-white rounded-lg py-3 pl-10 pr-4 outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all shadow-inner"
                    />
                  </div>
                </div>
              )}

              {/* Email */}
              <div className="relative group">
                <label className="text-sm font-medium text-slate-300 block mb-1">E-mail</label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-blue-500 transition-colors" size={18} />
                  <input 
                    type="email" 
                    placeholder="seu@email.com"
                    className="w-full bg-slate-900 border border-slate-700 text-white rounded-lg py-3 pl-10 pr-4 outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all shadow-inner"
                  />
                </div>
              </div>

              {/* Senha */}
              <div className="relative group">
                <div className="flex justify-between items-center mb-1">
                  <label className="text-sm font-medium text-slate-300">Senha</label>
                  {isLogin && (
                    <a href="#" className="text-xs text-blue-500 hover:text-blue-400 transition-colors">Esqueceu a senha?</a>
                  )}
                </div>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-blue-500 transition-colors" size={18} />
                  <input 
                    type="password" 
                    placeholder="••••••••"
                    className="w-full bg-slate-900 border border-slate-700 text-white rounded-lg py-3 pl-10 pr-4 outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all shadow-inner"
                  />
                </div>
                {!isLogin && <p className="text-[10px] text-slate-500 mt-1">Mínimo de 8 caracteres.</p>}
              </div>

              {/* Confirmar Senha (Apenas no Registro) */}
              {!isLogin && (
                <div className="relative group">
                  <label className="text-sm font-medium text-slate-300 block mb-1">Confirmar Senha</label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-blue-500 transition-colors" size={18} />
                    <input 
                      type="password" 
                      placeholder="••••••••"
                      className="w-full bg-slate-900 border border-slate-700 text-white rounded-lg py-3 pl-10 pr-4 outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all shadow-inner"
                    />
                  </div>
                </div>
              )}
            </div>

            {/* Checkbox Opções */}
            <div className="flex items-center">
              <input 
                type="checkbox" 
                id="terms" 
                className="w-4 h-4 rounded border-slate-700 bg-slate-900 text-blue-600 focus:ring-blue-500 focus:ring-offset-slate-900" 
              />
              <label htmlFor="terms" className="ml-2 text-sm text-slate-400">
                {isLogin ? 'Lembrar de mim' : (
                  <span>Eu concordo com os <a href="#" className="text-blue-500 underline">Termos</a> e <a href="#" className="text-blue-500 underline">Políticas</a></span>
                )}
              </label>
            </div>

            {/* Botão de Submissão */}
            <button className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-4 rounded-lg transition-all flex items-center justify-center gap-2 group shadow-lg shadow-blue-900/20 active:scale-[0.98]">
              {isLogin ? 'Entrar' : 'Criar Conta'}
              <ArrowRight size={18} className="group-hover:translate-x-1 transition-transform" />
            </button>
          </form>

          {/* Troca de Estado (Login/Registro) */}
          <p className="text-center text-slate-400 text-sm mt-8">
            {isLogin ? 'Não tem uma conta?' : 'Já possui uma conta?'}
            <button 
              onClick={() => setIsLogin(!isLogin)}
              className="ml-2 text-blue-500 font-semibold hover:text-blue-400 transition-colors"
            >
              {isLogin ? 'Registre-se' : 'Faça Login'}
            </button>
          </p>

        </div>
      </div>
    </div>
  );
};


export default AuthPage;