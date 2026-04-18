const TestBackground = () => {
  return (
    <div style={{ width: '100vw', height: '100vh', position: 'relative' }}>
      {/* Teste 1: Imagem direta com src */}
      <img 
        src="/logo.png" 
        alt="test1"
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          position: 'absolute',
          top: 0,
          left: 0
        }}
      />
      
      {/* Texto para verificação */}
      <div style={{
        position: 'absolute',
        zIndex: 10,
        top: '20px',
        left: '20px',
        color: 'white',
        backgroundColor: 'rgba(0,0,0,0.8)',
        padding: '20px',
        borderRadius: '5px'
      }}>
        <h1>Teste de Background</h1>
        <p>Se você consegue ver a imagem de fundo, o Vite está servindo arquivos corretamente.</p>
      </div>
    </div>
  );
};

export default TestBackground;
